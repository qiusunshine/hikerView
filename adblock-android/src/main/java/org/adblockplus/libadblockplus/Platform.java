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

package org.adblockplus.libadblockplus;

public class Platform implements Disposable
{
  private final Disposer disposer;
  protected final long ptr;

  static
  {
    System.loadLibrary(BuildConfig.nativeLibraryName);
    registerNatives();
  }

  /**
   * If an interface parameter value is null then a default implementation is
   * chosen.
   * If basePath is null then paths are not resolved to a full path, thus
   * current working directory is used.
   * @param logSystem LogSystem concrete implementation
   * @param fileSystem FileSystem concrete implementation
   * @param httpClient HttpClient concrete implementation
   * @param basePath base path for FileSystem (default C++ FileSystem implementation used)
   */
  public Platform(final LogSystem logSystem, final FileSystem fileSystem, final HttpClient httpClient, final String basePath)
  {
    this(ctor(logSystem, fileSystem, httpClient, basePath));
  }

  protected Platform(final long ptr)
  {
    this.ptr = ptr;
    this.disposer = new Disposer(this, new DisposeWrapper(ptr));
  }

  public void setUpJsEngine(final AppInfo appInfo, final long v8IsolateProviderPtr)
  {
    setUpJsEngine(this.ptr, appInfo, v8IsolateProviderPtr);
  }

  public void setUpJsEngine(final AppInfo appInfo)
  {
    setUpJsEngine(appInfo, 0L);
  }

  public JsEngine getJsEngine()
  {
    return new JsEngine(getJsEnginePtr(this.ptr));
  }

  public void setUpFilterEngine(final IsAllowedConnectionCallback isSubscriptionDownloadAllowedCallback)
  {
    setUpFilterEngine(this.ptr, isSubscriptionDownloadAllowedCallback);
  }

  public FilterEngine getFilterEngine()
  {
    // Initially FilterEngine is not constructed when Platform is instantiated
    // and in addition FilterEngine is being created asynchronously, the call
    // of `ensureFilterEngine` causes a construction of FilterEngine if it's
    // not created yet and waits for it.
    ensureFilterEngine(this.ptr);
    return new FilterEngine(this.ptr);
  }

  @Override
  public void dispose()
  {
    this.disposer.dispose();
  }

  private final static class DisposeWrapper implements Disposable
  {
    private final long ptr;

    public DisposeWrapper(final long ptr)
    {
      this.ptr = ptr;
    }

    @Override
    public void dispose()
    {
      dtor(this.ptr);
    }
  }

  private final static native void registerNatives();

  private final static native long ctor(LogSystem logSystem,
                                        FileSystem fileSystem,
                                        HttpClient httpClient,
                                        String basePath);

  private final static native void setUpJsEngine(long ptr, AppInfo appInfo, long v8IsolateProviderPtr);

  private final static native long getJsEnginePtr(long ptr);

  private final static native void setUpFilterEngine(long ptr, IsAllowedConnectionCallback isSubscriptionDownloadAllowedCallback);

  private final static native void ensureFilterEngine(long ptr);

  private final static native void dtor(long ptr);
}
