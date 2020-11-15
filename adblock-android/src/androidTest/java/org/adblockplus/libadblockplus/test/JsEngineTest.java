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
import org.adblockplus.libadblockplus.JsValue;
import org.adblockplus.libadblockplus.TestEventCallback;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JsEngineTest extends BaseJsEngineTest
{
  protected boolean isSame(final JsValue value1, final JsValue value2)
  {
    final List<JsValue> params = new ArrayList<>();
    params.add(value1);
    params.add(value2);

    return jsEngine
        .evaluate("(function(a, b) { return a == b })")
        .call(params)
        .asBoolean();
  }

  @Test
  public void testEvaluate()
  {
    jsEngine.evaluate("function hello() { return 'Hello'; }");
    JsValue result = jsEngine.evaluate("hello()");
    assertNotNull(result);
    assertTrue(result.isString());
    assertEquals("Hello", result.asString());
  }

  @Test
  public void testRuntimeExceptionIsThrown()
  {
    try
    {
      jsEngine.evaluate("doesnotexist()");
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }
  }

  @Test
  public void testCompileTimeExceptionIsThrown()
  {
    try
    {
      jsEngine.evaluate("'foo'bar'");
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }
  }

  @Test
  public void testValueCreation()
  {
    JsValue value;

    final String STRING_VALUE = "foo";
    value = jsEngine.newValue(STRING_VALUE);
    assertNotNull(value);
    assertTrue(value.isString());
    assertEquals(STRING_VALUE, value.asString());

    final long LONG_VALUE = 12345678901234L;
    value = jsEngine.newValue(LONG_VALUE);
    assertNotNull(value);
    assertTrue(value.isNumber());
    assertEquals(LONG_VALUE, value.asLong());

    final boolean BOOLEAN_VALUE = true;
    value = jsEngine.newValue(BOOLEAN_VALUE);
    assertNotNull(value);
    assertTrue(value.isBoolean());
    assertEquals(BOOLEAN_VALUE, value.asBoolean());
  }

  @Test
  public void testValueCopyString()
  {
    final String STRING_VALUE = "foo";

    JsValue value1 = jsEngine.newValue(STRING_VALUE);
    assertNotNull(value1);
    assertTrue(value1.isString());
    assertEquals(STRING_VALUE, value1.asString());

    JsValue value2 = jsEngine.newValue(STRING_VALUE);
    assertNotNull(value2);
    assertTrue(value2.isString());
    assertEquals(STRING_VALUE, value2.asString());

    assertTrue(isSame(value1, value2));
  }

  @Test
  public void testValueCopyLong()
  {
    final long LONG_VALUE = 12345678901234L;

    JsValue value1 = jsEngine.newValue(LONG_VALUE);
    assertNotNull(value1);
    assertTrue(value1.isNumber());
    assertEquals(LONG_VALUE, value1.asLong());

    JsValue value2 = jsEngine.newValue(LONG_VALUE);
    assertNotNull(value2);
    assertTrue(value2.isNumber());
    assertEquals(LONG_VALUE, value2.asLong());

    assertTrue(isSame(value1, value2));
  }

  @Test
  public void testValueCopyBool()
  {
    final boolean BOOL_VALUE = true;

    JsValue value1 = jsEngine.newValue(BOOL_VALUE);
    assertNotNull(value1);
    assertTrue(value1.isBoolean());
    assertEquals(BOOL_VALUE, value1.asBoolean());

    JsValue value2 = jsEngine.newValue(BOOL_VALUE);
    assertNotNull(value2);
    assertTrue(value2.isBoolean());
    assertEquals(BOOL_VALUE, value2.asBoolean());

    assertTrue(isSame(value1, value2));
  }

  @Test
  public void testEventCallbacks()
  {
    // Not using Mockito as `JniCallbackBase.javaVM` stays uninitialized causing NPE
    final TestEventCallback eventCallback = new TestEventCallback();

    // Trigger event without a callback
    eventCallback.reset();
    jsEngine.evaluate("_triggerEvent('foobar')");
    assertFalse(eventCallback.isCalled());

    // Set callback
    eventCallback.reset();
    final String EVENT_NAME = "foobar";
    jsEngine.setEventCallback(EVENT_NAME, eventCallback);
    jsEngine.evaluate("_triggerEvent('foobar', 1, 'x', true)");
    assertTrue(eventCallback.isCalled());
    assertNotNull(eventCallback.getParams());
    assertEquals(3, eventCallback.getParams().size());
    assertEquals(1L, eventCallback.getParams().get(0).asLong());
    assertEquals("x", eventCallback.getParams().get(1).asString());
    assertTrue(eventCallback.getParams().get(2).asBoolean());

    // Trigger a different event
    eventCallback.reset();
    jsEngine.evaluate("_triggerEvent('barfoo')");
    assertFalse(eventCallback.isCalled());

    // Remove callback
    eventCallback.reset();
    jsEngine.removeEventCallback(EVENT_NAME);
    jsEngine.evaluate("_triggerEvent('foobar')");
    assertFalse(eventCallback.isCalled());
  }

  @Test
  public void testGlobalProperty()
  {
    final String PROPERTY = "foo";
    final String VALUE = "bar";

    jsEngine.setGlobalProperty(PROPERTY, jsEngine.newValue(VALUE));
    JsValue value = jsEngine.evaluate(PROPERTY);
    assertNotNull(value);
    assertTrue(value.isString());
    assertEquals(VALUE, value.asString());
  }
}
