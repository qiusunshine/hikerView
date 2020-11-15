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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface AdblockEngineProvider
{
  /**
   * Register AdblockEngine client
   * @param asynchronous If `true` engines will be created in background thread without locking of
   *                     current thread. Use waitForReady() before getEngine() later.
   *                     If `false` locks current thread.
   * @return if a new instance is allocated
   */
  boolean retain(boolean asynchronous);

  /**
   * Wait until everything is ready (used for `retain(true)`)
   * Warning: locks current thread
   */
  void waitForReady();

  /**
   * Return AdblockEngine instance
   * @return AdblockEngine instance. Can be `null` if not yet retained or already released
   */
  AdblockEngine getEngine();

  /**
   * Unregister AdblockEngine client
   * @return `true` if the last instance is destroyed
   */
  boolean release();

  /**
   * Get registered clients count
   * @return registered clients count
   */
  int getCounter();

  /**
   * Get lock object to prevent AdblockEngine reference from being changed
   * @return read lock object
   */
  ReentrantReadWriteLock.ReadLock getReadEngineLock();

  interface EngineCreatedListener
  {
    void onAdblockEngineCreated(AdblockEngine engine);
  }

  interface BeforeEngineDisposedListener
  {
    void onBeforeAdblockEngineDispose();
  }

  interface EngineDisposedListener
  {
    void onAdblockEngineDisposed();
  }

  AdblockEngineProvider addEngineCreatedListener(EngineCreatedListener listener);

  void removeEngineCreatedListener(EngineCreatedListener listener);

  void clearEngineCreatedListeners();

  AdblockEngineProvider addBeforeEngineDisposedListener(BeforeEngineDisposedListener listener);

  void removeBeforeEngineDisposedListener(BeforeEngineDisposedListener listener);

  void clearBeforeEngineDisposedListeners();

  AdblockEngineProvider addEngineDisposedListener(EngineDisposedListener listener);

  void removeEngineDisposedListener(EngineDisposedListener listener);

  void clearEngineDisposedListeners();
}
