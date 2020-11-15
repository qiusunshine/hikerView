/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.adblockplus.libadblockplus.android;

import org.adblockplus.libadblockplus.IsAllowedConnectionCallback;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import timber.log.Timber;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides single instance of AdblockEngine shared between registered clients
 */
public class SingleInstanceEngineProvider implements AdblockEngineProvider
{
  private Context context;
  private String basePath;
  private boolean developmentBuild;
  private AtomicReference<String> preloadedPreferenceName = new AtomicReference<>();
  private AtomicReference<Map<String, Integer>> urlToResourceIdMap = new AtomicReference<>();
  private AtomicReference<AdblockEngine> engineReference = new AtomicReference<>();
  private AtomicLong v8IsolateProviderPtr = new AtomicLong(0);
  private List<EngineCreatedListener> engineCreatedListeners = new CopyOnWriteArrayList<>();
  private List<BeforeEngineDisposedListener> beforeEngineDisposedListeners = new CopyOnWriteArrayList<>();
  private List<EngineDisposedListener> engineDisposedListeners = new CopyOnWriteArrayList<>();
  private final ReentrantReadWriteLock engineLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock referenceCounterLock = new ReentrantReadWriteLock();
  private final ExecutorService executorService;
  private boolean disabledByDefault = false;

  /*
    Simple ARC management for AdblockEngine
    Use `retain` and `release`
   */

  private AtomicInteger referenceCounter = new AtomicInteger(0);

  // shutdowns `ExecutorService` instance on system shutdown
  private static class ExecutorServiceShutdownHook extends Thread
  {
    private final ExecutorService executorService;

    private ExecutorServiceShutdownHook(final ExecutorService executorService)
    {
      Timber.w("Hooking on executor service %s", executorService);
      this.executorService = executorService;
    }

    @Override
    public void run()
    {
      Timber.w("Shutting down executor service %s", executorService);
      executorService.shutdown();
    }
  }

  /**
   * Init with context
   * @param context application context
   * @param basePath file system root to store files
   *
   *                 Adblock Plus library will download subscription files and store them on
   *                 the path passed. The path should exist and the directory content should not be
   *                 cleared out occasionally. Using `context.getCacheDir().getAbsolutePath()` is not
   *                 recommended because it can be cleared by the system.
   * @param developmentBuild debug or release?
   */
  public SingleInstanceEngineProvider(Context context, String basePath, boolean developmentBuild)
  {
    this.context = context.getApplicationContext();
    this.basePath = basePath;
    this.developmentBuild = developmentBuild;
    this.executorService = createExecutorService();
  }

  protected ExecutorService createExecutorService()
  {
    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    Runtime.getRuntime().addShutdownHook(new ExecutorServiceShutdownHook(executorService));
    return executorService;
  }

  /**
   * Use preloaded subscriptions
   * @param preferenceName Shared Preferences name to store intercepted requests stats
   * @param urlToResourceIdMap URL to Android resource id map
   * @return this (for method chaining)
   */
  public SingleInstanceEngineProvider preloadSubscriptions(String preferenceName,
                                                           Map<String, Integer> urlToResourceIdMap)
  {
    this.preloadedPreferenceName.set(preferenceName);
    this.urlToResourceIdMap.set(urlToResourceIdMap);
    return this;
  }

  public SingleInstanceEngineProvider useV8IsolateProvider(long ptr)
  {
    this.v8IsolateProviderPtr.set(ptr);
    return this;
  }

  /**
   * Will create filter engine disabled by default. This means subscriptions will be updated only
   * when setEnabled(true) will be called. This function configures only default engine state. If
   * other state is stored in settings, it will be preferred.
   */
  public SingleInstanceEngineProvider setDisabledByDefault()
  {
    this.disabledByDefault = true;
    return this;
  }

  @Override
  public SingleInstanceEngineProvider addEngineCreatedListener(EngineCreatedListener listener)
  {
    this.engineCreatedListeners.add(listener);
    return this;
  }

  @Override
  public void removeEngineCreatedListener(EngineCreatedListener listener)
  {
    this.engineCreatedListeners.remove(listener);
  }

  @Override
  public void clearEngineCreatedListeners()
  {
    this.engineCreatedListeners.clear();
  }

  @Override
  public SingleInstanceEngineProvider addBeforeEngineDisposedListener(BeforeEngineDisposedListener listener)
  {
    this.beforeEngineDisposedListeners.add(listener);
    return this;
  }

  @Override
  public void removeBeforeEngineDisposedListener(BeforeEngineDisposedListener listener)
  {
    this.beforeEngineDisposedListeners.remove(listener);
  }

  @Override
  public void clearBeforeEngineDisposedListeners()
  {
    this.beforeEngineDisposedListeners.clear();
  }

  @Override
  public SingleInstanceEngineProvider addEngineDisposedListener(EngineDisposedListener listener)
  {
    this.engineDisposedListeners.add(listener);
    return this;
  }

  @Override
  public void removeEngineDisposedListener(EngineDisposedListener listener)
  {
    this.engineDisposedListeners.remove(listener);
  }

  @Override
  public void clearEngineDisposedListeners()
  {
    this.engineDisposedListeners.clear();
  }

  private void createAdblock()
  {
    Timber.d("Creating adblock engine ...");
    ConnectivityManager connectivityManager =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    IsAllowedConnectionCallback isAllowedConnectionCallback =
        new IsAllowedConnectionCallbackImpl(connectivityManager);

    AdblockEngine.Builder builder = AdblockEngine
        .builder(
            AdblockEngine.generateAppInfo(context, developmentBuild),
            basePath)
        .setIsAllowedConnectionCallback(isAllowedConnectionCallback)
        .enableElementHiding(true);

    long v8IsolateProviderPtrLocal = v8IsolateProviderPtr.get();
    if (v8IsolateProviderPtrLocal != 0)
    {
      builder.useV8IsolateProvider(v8IsolateProviderPtrLocal);
    }

    String preloadedPreferenceNameLocal = preloadedPreferenceName.get();
    Map<String, Integer> urlToResourceIdMapLocal = urlToResourceIdMap.get();
    // if preloaded subscriptions provided
    if (preloadedPreferenceNameLocal != null)
    {
      SharedPreferences preloadedSubscriptionsPrefs = context.getSharedPreferences(
          preloadedPreferenceNameLocal,
          Context.MODE_PRIVATE);
      builder.preloadSubscriptions(
          context,
          urlToResourceIdMapLocal,
          new AndroidHttpClientResourceWrapper.SharedPrefsStorage(preloadedSubscriptionsPrefs));
    }

    AdblockEngine engine = builder.build();
    Timber.d("AdblockHelper engine created");

    if (disabledByDefault)
    {
      engine.configureDisabledByDefault(context);
    }

    engineReference.set(engine);

    // sometimes we need to init AdblockEngine instance, eg. set user settings
    for (EngineCreatedListener listener : engineCreatedListeners)
    {
      listener.onAdblockEngineCreated(engine);
    }
  }

  @Override
  public boolean retain(final boolean asynchronous)
  {
    final Future future;
    referenceCounterLock.writeLock().lock();
    try
    {
      final boolean firstInstance = (referenceCounter.getAndIncrement() == 0);
      if (!firstInstance)
      {
        return false;
      }
      future = scheduleTask(retainTask);
    }
    finally
    {
      referenceCounterLock.writeLock().unlock();
    }

    if (!asynchronous)
    {
      waitForTask(future);
    }
    return true;
  }

  private final Runnable retainTask = new Runnable()
  {
    @Override
    public void run()
    {
      Timber.w("Waiting for lock in " + Thread.currentThread());
      engineLock.writeLock().lock();

      try
      {
        createAdblock();
      }
      finally
      {
        engineLock.writeLock().unlock();
      }
    }
  };

  // the task does nothing and can be used as a way to wait
  // for all the current tasks to be finished
  private final Runnable waitForTheTasksTask = new Runnable()
  {
    @Override
    public void run()
    {
      // nothing
    }
  };

  @Override
  public void waitForReady()
  {
    Timber.d("Waiting for ready in %s", Thread.currentThread());
    waitForTask(scheduleTask(waitForTheTasksTask));
    Timber.d("Ready");
  }

  @Override
  public AdblockEngine getEngine()
  {
    return engineReference.get();
  }

  @Override
  public boolean release()
  {
    final Future future;
    referenceCounterLock.writeLock().lock();
    try
    {
      final boolean lastInstance = (referenceCounter.decrementAndGet() == 0);
      if (!lastInstance)
      {
        return false;
      }
      future = scheduleTask(releaseTask);
    }
    finally
    {
      referenceCounterLock.writeLock().unlock();
    }

    waitForTask(future); // release() is always synchronous
    return true;
  }

  private Future scheduleTask(final Runnable task)
  {
    return executorService.submit(task);
  }

  private void waitForTask(final Future future) throws RuntimeException
  {
    try
    {
      future.get(); // block the thread and wait
    }
    catch (final Exception e)
    {
      Timber.e(e);
      throw new RuntimeException(e);
    }
  }

  private final Runnable releaseTask = new Runnable()
  {
    @Override
    public void run()
    {
      Timber.w("Waiting for lock in " + Thread.currentThread());
      engineLock.writeLock().lock();

      try
      {
        disposeAdblock();
      }
      finally
      {
        engineLock.writeLock().unlock();
      }
    }
  };

  private void disposeAdblock()
  {
    Timber.w("Disposing adblock engine");

    for (BeforeEngineDisposedListener listener : beforeEngineDisposedListeners)
    {
      listener.onBeforeAdblockEngineDispose();
    }

    engineReference.getAndSet(null).dispose();

    // sometimes we need to deinit something after AdblockEngine instance disposed
    // eg. release user settings
    for (EngineDisposedListener listener : engineDisposedListeners)
    {
      listener.onAdblockEngineDisposed();
    }
  }

  @Override
  public int getCounter()
  {
    referenceCounterLock.readLock().lock();
    try
    {
      return referenceCounter.get();
    }
    finally
    {
      referenceCounterLock.readLock().unlock();
    }
  }

  @Override
  public ReentrantReadWriteLock.ReadLock getReadEngineLock()
  {
    Timber.d("getReadEngineLock() called from " + Thread.currentThread());
    return engineLock.readLock();
  }
}
