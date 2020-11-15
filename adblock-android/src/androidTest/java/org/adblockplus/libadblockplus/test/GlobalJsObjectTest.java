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

import org.adblockplus.libadblockplus.AdblockPlusException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GlobalJsObjectTest extends BaseJsEngineTest
{
  public static final int SLEEP_INTERVAL_MS = 200;

  @Test
  public void testSetTimeout() throws InterruptedException
  {
    jsEngine.evaluate("let foo; setTimeout(function() {foo = 'bar';}, 100)");
    assertTrue(jsEngine.evaluate("foo").isUndefined());
    Thread.sleep(SLEEP_INTERVAL_MS);
    assertEquals("bar", jsEngine.evaluate("foo").asString());
  }

  @Test
  public void testSetTimeoutWithArgs() throws InterruptedException
  {
    jsEngine.evaluate("let foo; setTimeout(function(s) {foo = s;}, 100, 'foobar')");
    assertTrue(jsEngine.evaluate("foo").isUndefined());
    Thread.sleep(SLEEP_INTERVAL_MS);
    assertEquals("foobar", jsEngine.evaluate("foo").asString());
  }

  @Test
  public void testSetTimeoutWithInvalidArgs()
  {
    try
    {
      jsEngine.evaluate("setTimeout()");
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }

    try
    {
      jsEngine.evaluate("setTimeout('', 1)");
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }
  }

  @Test
  public void testSetMultipleTimeouts() throws InterruptedException
  {
    jsEngine.evaluate("let foo = []");
    jsEngine.evaluate("setTimeout(function(s) {foo.push('1');}, 100)");
    jsEngine.evaluate("setTimeout(function(s) {foo.push('2');}, 150)");
    Thread.sleep(SLEEP_INTERVAL_MS);
    assertEquals("1,2", jsEngine.evaluate("foo").asString());
  }
}
