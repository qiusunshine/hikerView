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

import org.adblockplus.libadblockplus.LogSystem;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConsoleJsObjectTest extends BaseJsEngineTest
{
  protected LogSystem mockLogSystem;

  @Override
  public void setUp()
  {
    mockLogSystem = mock(LogSystem.class);
    setUpLogSystem(mockLogSystem);
    super.setUp();
  }

  @Test
  public void testConsoleLogCall()
  {
    jsEngine.evaluate("\n\nconsole.log('foo', 'bar');\n\n", "eval");
    verify(mockLogSystem).logCallback(LogSystem.LogLevel.LOG, "foo bar", "eval:3");
  }

  @Test
  public void testConsoleDebugCall()
  {
    jsEngine.evaluate("console.debug('foo', 'bar')");
    verify(mockLogSystem).logCallback(LogSystem.LogLevel.LOG, "foo bar", ":1");
  }

  @Test
  public void testConsoleInfoCall()
  {
    jsEngine.evaluate("console.info('foo', 'bar')");
    verify(mockLogSystem).logCallback(LogSystem.LogLevel.INFO, "foo bar", ":1");
  }

  @Test
  public void testConsoleWarnCall()
  {
    jsEngine.evaluate("console.warn('foo', 'bar')");
    verify(mockLogSystem).logCallback(LogSystem.LogLevel.WARN, "foo bar", ":1");
  }

  @Test
  public void testConsoleErrorCall()
  {
    jsEngine.evaluate("console.error('foo', 'bar')");
    verify(mockLogSystem).logCallback(LogSystem.LogLevel.ERROR, "foo bar", ":1");
  }

  @Test
  public void testConsoleTraceCall()
  {
    jsEngine.evaluate(
        "\n" +
        "function foo()\n" +
        "{\n" +
        "   (function() {\n" +
        "       console.trace();\n" +
        "   })();\n" +
        "}\n" +
        "foo();", "eval");
    verify(mockLogSystem).logCallback(
        LogSystem.LogLevel.TRACE,
        "1: /* anonymous */() at eval:5\n" +
        "2: foo() at eval:6\n" +
        "3: /* anonymous */() at eval:8\n",
        "");
  }

  @Test
  public void testConsoleOneCall()
  {
    jsEngine.evaluate("\n\nconsole.log('foo', 'bar');\n\n", "eval");
    verify(mockLogSystem, times(1))
        .logCallback(LogSystem.LogLevel.LOG, "foo bar", "eval:3");
  }

  @Test
  public void testConsoleMultipleCalls()
  {
    jsEngine.evaluate("\n\nconsole.log('foo', 'bar');console.log('foo', 'bar');\n\n", "eval");
    verify(mockLogSystem, times(2))
        .logCallback(LogSystem.LogLevel.LOG, "foo bar", "eval:3");
  }
}
