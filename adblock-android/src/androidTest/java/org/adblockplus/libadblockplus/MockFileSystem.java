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

import org.adblockplus.libadblockplus.android.Utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class MockFileSystem extends FileSystem
{
  public ExecutorService executorService;
  public CountDownLatch nextOperationStartedLatch;
  public CountDownLatch nextOperationFinishedLatch;

  public boolean success = true;
  public boolean exception;
  public Charset charset = Charset.forName("UTF-8");
  public String contentToRead;
  public String lastWrittenFile;
  public String lastWrittenContent;
  public String movedFrom;
  public String movedTo;
  public String removedFile;
  public String statFile;
  public boolean statExists;
  public long statLastModified;

  /**
   * Tasks are executed in current thread
   */
  public MockFileSystem()
  {
  }

  /**
   * Tasks will be executed in background thread by executorService
   * @param executorService Executor Service
   */
  public MockFileSystem(ExecutorService executorService)
  {
    this();
    this.executorService = executorService;
  }

  // schedule the task if working in background mode, execute the task immediately otherwise.
  protected void perform(Runnable task)
  {
    // notify call started
    if (nextOperationStartedLatch != null)
    {
      nextOperationStartedLatch.countDown();
      nextOperationStartedLatch = null;
    }

    if (executorService != null)
    {
      executorService.submit(task);
    }
    else
    {
      task.run();
    }

    // notify call finished
    if (nextOperationFinishedLatch != null)
    {
      nextOperationFinishedLatch.countDown();
      nextOperationFinishedLatch = null;
    }
  }

  @Override
  public void read(final String filename,
                   final ReadCallback doneCallback,
                   final Callback errorCallback)
  {
    perform(new Runnable()
    {
      @Override
      public void run()
      {
        if (exception)
        {
          throw new RuntimeException("Exception simulation");
        }

        if (success)
        {
          doneCallback.onFinished(Utils.stringToByteBuffer(contentToRead, charset));
        }
        else
        {
          errorCallback.onFinished("Unable to read " + filename);
        }
      }
    });
  }

  @Override
  public void write(final String filename,
                    final ByteBuffer data,
                    final Callback callback)
  {
    perform(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          if (exception)
          {
            throw new RuntimeException("Exception simulation");
          }

          if (!success)
          {
            callback.onFinished("Unable to write to " + filename);
            return;
          }

          lastWrittenFile = filename;
          lastWrittenContent = new String(Utils.byteBufferToByteArray(data), charset);
          callback.onFinished(null);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  public void move(final String fromFilename,
                   final String toFilename,
                   final Callback callback)
  {
    perform(new Runnable()
    {
      @Override
      public void run()
      {
        if (exception)
        {
          throw new RuntimeException("Exception simulation");
        }

        if (!success)
        {
          callback.onFinished("Unable to move " + fromFilename + " to " + toFilename);
          return;
        }

        movedFrom = fromFilename;
        movedTo = toFilename;
        callback.onFinished(null);
      }
    });
  }

  @Override
  public void remove(final String filename,
                     final Callback callback)
  {
    perform(new Runnable()
    {
      @Override
      public void run()
      {
        if (exception)
        {
          throw new RuntimeException("Exception simulation");
        }

        if (!success)
        {
          callback.onFinished("Unable to remove " + filename);
          return;
        }

        removedFile = filename;
        callback.onFinished(null);
      }
    });
  }

  @Override
  public void stat(final String filename,
                   final StatCallback callback)
  {
    perform(new Runnable()
    {
      @Override
      public void run()
      {
        if (exception)
        {
          throw new RuntimeException("Exception simulation");
        }

        StatResult result = null;
        String error = null;
        if (!success)
        {
          error = "Unable to stat " + filename;
        }
        else
        {
          statFile = filename;
          result = new StatResult(statExists, statLastModified);
        }
        callback.onFinished(result, error);
      }
    });
  }
}
