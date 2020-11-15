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
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JsValueTest extends BaseJsEngineTest
{
  private void assertAsListThrowsException(final JsValue value)
  {
    try
    {
      value.asList();
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }
  }

  private void assertCallThrowsException(final JsValue value)
  {
    try
    {
      value.call();
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }
  }

  protected void assertThrowsExceptions(final JsValue value)
  {
    assertAsListThrowsException(value);

    try
    {
      value.getProperty("foo");
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }

    try
    {
      value.setProperty("foo", jsEngine.newValue(false));
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }

    try
    {
      value.getJsClass();
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }

    try
    {
      value.getOwnPropertyNames();
      fail();
    }
    catch (AdblockPlusException e)
    {
      // expected exception
    }

    assertCallThrowsException(value);
  }

  @Test
  public void testUndefinedValue()
  {
    final String UNDEFINED = "undefined";

    JsValue value = jsEngine.evaluate(UNDEFINED);
    assertNotNull(value);
    assertTrue(value.isUndefined());
    assertFalse(value.isNull());
    assertFalse(value.isString());
    assertFalse(value.isBoolean());
    assertFalse(value.isNumber());
    assertFalse(value.isObject());
    assertFalse(value.isArray());
    assertFalse(value.isFunction());
    assertEquals(UNDEFINED, value.asString());
    assertFalse(value.asBoolean());
    assertThrowsExceptions(value);
  }

  @Test
  public void testNullValue()
  {
    final String NULL = "null";

    JsValue value = jsEngine.evaluate(NULL);
    assertNotNull(value);
    assertFalse(value.isUndefined());
    assertTrue(value.isNull());
    assertFalse(value.isString());
    assertFalse(value.isBoolean());
    assertFalse(value.isNumber());
    assertFalse(value.isObject());
    assertFalse(value.isArray());
    assertFalse(value.isFunction());
    assertEquals(NULL, value.asString());
    assertFalse(value.asBoolean());
    assertThrowsExceptions(value);
  }

  @Test
  public void testStringValue()
  {
    final String STRING_VALUE = "123";

    JsValue value = jsEngine.evaluate("'" + STRING_VALUE +"'");
    assertNotNull(value);
    assertFalse(value.isUndefined());
    assertFalse(value.isNull());
    assertTrue(value.isString());
    assertFalse(value.isBoolean());
    assertFalse(value.isNumber());
    assertFalse(value.isObject());
    assertFalse(value.isArray());
    assertFalse(value.isFunction());
    assertEquals(STRING_VALUE, value.asString());
    assertTrue(value.asBoolean());
    assertThrowsExceptions(value);
  }

  @Test
  public void testLongValue()
  {
    final long LONG_VALUE = 12345678901234L;
    final String LONG_STR_VALUE = String.valueOf(LONG_VALUE);

    JsValue value = jsEngine.evaluate(LONG_STR_VALUE);
    assertNotNull(value);
    assertFalse(value.isUndefined());
    assertFalse(value.isNull());
    assertFalse(value.isString());
    assertFalse(value.isBoolean());
    assertTrue(value.isNumber());
    assertFalse(value.isObject());
    assertFalse(value.isArray());
    assertFalse(value.isFunction());
    assertEquals(LONG_STR_VALUE, value.asString());
    assertEquals(LONG_VALUE, value.asLong());
    assertTrue(value.asBoolean());
    assertThrowsExceptions(value);
  }

  @Test
  public void testBoolValue()
  {
    final boolean BOOL_VALUE = true;
    final String BOOL_STR_VALUE = String.valueOf(BOOL_VALUE);

    JsValue value = jsEngine.evaluate(BOOL_STR_VALUE);
    assertNotNull(value);
    assertFalse(value.isUndefined());
    assertFalse(value.isNull());
    assertFalse(value.isString());
    assertTrue(value.isBoolean());
    assertFalse(value.isNumber());
    assertFalse(value.isObject());
    assertFalse(value.isArray());
    assertFalse(value.isFunction());
    assertEquals(BOOL_STR_VALUE, value.asString());
    assertEquals(BOOL_VALUE, value.asBoolean());
    assertThrowsExceptions(value);
  }

  @Test
  public void testObjectValue()
  {
    final String OBJECT_VALUE = "\n" +
        "function Foo() {\n" +
        "  this.x = 2;\n" +
        "  this.toString = function() {return 'foo';};\n" +
        "  this.valueOf = function() {return 123;};\n" +
        "};\n" +
        "new Foo()";

    JsValue value = jsEngine.evaluate(OBJECT_VALUE);
    assertNotNull(value);
    assertFalse(value.isUndefined());
    assertFalse(value.isNull());
    assertFalse(value.isString());
    assertFalse(value.isBoolean());
    assertFalse(value.isNumber());
    assertTrue(value.isObject());
    assertFalse(value.isArray());
    assertFalse(value.isFunction());
    assertEquals("foo", value.asString());
    assertEquals(123L, value.asLong());
    assertAsListThrowsException(value);
    assertEquals(2L, value.getProperty("x").asLong());
    value.setProperty("x", jsEngine.newValue(12L));
    assertEquals(12L, value.getProperty("x").asLong());
    assertEquals("Foo", value.getJsClass());
    final List<String> ownPropertyNames = value.getOwnPropertyNames();
    assertNotNull(ownPropertyNames);
    assertEquals(3, ownPropertyNames.size());
    assertTrue(value.getProperty("bar").isUndefined());
  }

  @Test
  public void testArrayValue()
  {
    final String ARRAY_VALUE = "5,8,12";

    JsValue value = jsEngine.evaluate("[" + ARRAY_VALUE + "]");
    assertNotNull(value);
    assertFalse(value.isUndefined());
    assertFalse(value.isNull());
    assertFalse(value.isString());
    assertFalse(value.isBoolean());
    assertFalse(value.isNumber());
    assertTrue(value.isObject());
    assertTrue(value.isArray());
    assertFalse(value.isFunction());
    assertEquals(ARRAY_VALUE, value.asString());
    assertTrue(value.asBoolean());
    final List<JsValue> list = value.asList();
    assertNotNull(list);
    assertEquals(3, list.size());
    assertEquals(8L, list.get(1).asLong());
    assertEquals(3, value.getProperty("length").asLong());
    assertEquals("Array", value.getJsClass());
    assertCallThrowsException(value);
  }

  @Test
  public void testFunctionValue()
  {
    final String FUNCTION_VALUE = "(function(foo, bar) {return this.x + '/' + foo + '/' + bar;})";

    JsValue value = jsEngine.evaluate(FUNCTION_VALUE);
    assertNotNull(value);
    assertFalse(value.isUndefined());
    assertFalse(value.isNull());
    assertFalse(value.isString());
    assertFalse(value.isBoolean());
    assertFalse(value.isNumber());
    assertTrue(value.isObject());
    assertFalse(value.isArray());
    assertTrue(value.isFunction());
    assertTrue(value.asBoolean());
    assertAsListThrowsException(value);
    assertEquals(2L, value.getProperty("length").asLong());

    JsValue thisPtr = jsEngine.evaluate("({x:2})");
    List<JsValue> params = new LinkedList<>();
    params.add(jsEngine.newValue(5L));
    params.add(jsEngine.newValue("xyz"));
    JsValue result = value.call(params, thisPtr);
    assertNotNull(result);
    assertTrue(result.isString());
    assertEquals("2/5/xyz", result.asString());
  }
}
