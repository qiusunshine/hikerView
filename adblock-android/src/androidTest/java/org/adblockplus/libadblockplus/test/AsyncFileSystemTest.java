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

package org.adblockplus.libadblockplus.test;

import android.os.SystemClock;

import org.adblockplus.libadblockplus.MockFileSystem;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AsyncFileSystemTest extends BaseJsEngineTest
{
  protected final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
  protected final ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, tasks);
  protected final MockFileSystem mockFileSystem = new MockFileSystem(executorService);

  @Override
  public void setUp()
  {
    setUpFileSystem(mockFileSystem);
    super.setUp();
  }

  @Test
  public void testWriteAfterPlatformReleased() throws InterruptedException
  {
    final CountDownLatch unlockLatch = new CountDownLatch(1);
    final CountDownLatch startedLatch = new CountDownLatch(1);
    final Runnable latchTask = new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          startedLatch.countDown();
          unlockLatch.await();
        }
        catch (InterruptedException e)
        {
          // ignored
        }
      }
    };

    // block all FileSystem operations
    // (since it's single thread executor and first task blocked by latch `unlockLatch`)
    mockFileSystem.executorService.submit(latchTask);

    // wait for latch-blocked task to be started
    startedLatch.await();

    assertEquals(0, tasks.size());

    final CountDownLatch writeFinishedLatch = new CountDownLatch(1);
    mockFileSystem.nextOperationFinishedLatch = writeFinishedLatch;

    jsEngine.evaluate("let error = true; _fileSystem.write('foo', 'bar', function(e) {error = e})");

    // make sure `write` scheduled by libadblockplus and put in tasks queue
    writeFinishedLatch.await();

    // release the platform
    tearDown();

    // wait for platform to be released for sure
    SystemClock.sleep(5 * 1000);

    assertNull(mockFileSystem.lastWrittenFile); // `write` not yet called
    assertEquals(1, tasks.size()); // 1 pending `write` task from file system
    assertFalse(tasks.contains(latchTask));
    unlockLatch.countDown(); // unblock FileSystem tasks thread

    SystemClock.sleep(1 * 1000); // wait for `write` task executed on background thread
    assertNotNull(mockFileSystem.lastWrittenFile);
    assertEquals(0, tasks.size());

    assertNotNull(mockFileSystem.lastWrittenFile); // `write` called
    assertNotNull(mockFileSystem.lastWrittenContent);
    assertEquals("bar", mockFileSystem.lastWrittenContent);
  }
}
