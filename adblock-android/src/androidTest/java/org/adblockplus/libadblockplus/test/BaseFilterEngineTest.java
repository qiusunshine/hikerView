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

import org.adblockplus.libadblockplus.FilterEngine;

public abstract class BaseFilterEngineTest extends BaseJsEngineTest
{
  protected FilterEngine filterEngine;
  protected static final int SLEEP_STEP_MS = 50; // 50 ms
  protected static final int SLEEP_MAX_TIME_MS = 1 * 60 * 1000; // 1 minute

  private void setUpFilterEngine()
  {
    filterEngine = platform.getFilterEngine(); // in C++ actually creates new FilterEngine instance
  }

  @Override
  public void setUp()
  {
    super.setUp();
    setUpFilterEngine();
  }

  private void tearDownFilterEngine()
  {
    filterEngine = null;
  }

  @Override
  public void tearDown()
  {
    tearDownFilterEngine();
    super.tearDown();
  }

  protected void waitForDefined(final String property)
  {
    int sleptMs = 0;
    do
    {
      SystemClock.sleep(SLEEP_STEP_MS);
      sleptMs += SLEEP_STEP_MS;
      if (sleptMs > SLEEP_MAX_TIME_MS)
      {
        throw new RuntimeException("WebRequest max sleep time exceeded");
      }
    }
    while (jsEngine.evaluate(property).isUndefined());
  }
}
