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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;

public final class Disposer extends WeakReference<Disposable>
{
  static final ReferenceQueue<Disposable> referenceQueue = new ReferenceQueue<>();
  private static final HashSet<Disposer> disposerSet = new HashSet<>();
  private final Disposable disposable;
  private volatile boolean disposed = false;

  static
  {
    final Thread thread = new Thread(new Cleaner());
    thread.setName(Cleaner.class.getCanonicalName());
    thread.setDaemon(true);
    thread.start();
  }

  public Disposer(final Disposable referent, final Disposable disposable)
  {
    super(referent, referenceQueue);
    this.disposable = disposable;

    synchronized (disposerSet)
    {
      disposerSet.add(this);
    }
  }

  public synchronized void dispose()
  {
    if (!this.disposed)
    {
      try
      {
        this.disposable.dispose();
      }
      catch (final Throwable t)
      {
        // catch to set state to 'disposed' on all circumstances
      }

      this.disposed = true;
      synchronized (disposerSet)
      {
        disposerSet.remove(this);
      }
    }
  }

  private static final class Cleaner implements Runnable
  {
    public Cleaner()
    {
      //
    }

    @Override
    public void run()
    {
      for (;;)
      {
        try
        {
          ((Disposer) Disposer.referenceQueue.remove()).dispose();
        }
        catch (final Throwable t)
        {
          // ignored
        }
      }
    }
  }
}
