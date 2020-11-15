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

import java.util.Collections;
import java.util.List;

public class JsValue implements Disposable
{
  private final Disposer disposer;
  protected final long ptr;

  static
  {
    System.loadLibrary(BuildConfig.nativeLibraryName);
    registerNatives();
  }

  protected JsValue(final long ptr)
  {
    this.ptr = ptr;
    this.disposer = new Disposer(this, new DisposeWrapper(ptr));
  }

  @Override
  public void dispose()
  {
    this.disposer.dispose();
  }

  public boolean isUndefined()
  {
    return isUndefined(this.ptr);
  }

  public boolean isNull()
  {
    return isNull(this.ptr);
  }

  public boolean isString()
  {
    return isString(this.ptr);
  }

  public boolean isNumber()
  {
    return isNumber(this.ptr);
  }

  public boolean isBoolean()
  {
    return isBoolean(this.ptr);
  }

  public boolean isObject()
  {
    return isObject(this.ptr);
  }

  public boolean isArray()
  {
    return isArray(this.ptr);
  }

  public boolean isFunction()
  {
    return isFunction(this.ptr);
  }

  public String asString()
  {
    return asString(this.ptr);
  }

  public long asLong()
  {
    return asLong(this.ptr);
  }

  public boolean asBoolean()
  {
    return asBoolean(this.ptr);
  }

  protected long[] convertToPtrArray(final List<JsValue> params)
  {
    long[] paramPtrs = new long[params.size()];
    for (int i = 0; i < params.size(); i++)
    {
      paramPtrs[i] = params.get(i).ptr;
    }
    return paramPtrs;
  }

  public JsValue call(final List<JsValue> params)
  {
    return call(this.ptr, convertToPtrArray(params));
  }

  public JsValue call(final List<JsValue> params, final JsValue thisValue)
  {
    return call(this.ptr, convertToPtrArray(params), thisValue.ptr);
  }

  public JsValue call()
  {
    return this.call(Collections.<JsValue>emptyList());
  }

  public JsValue getProperty(final String name)
  {
    return getProperty(this.ptr, name);
  }

  public void setProperty(final String name, final JsValue value)
  {
    setProperty(this.ptr, name, value.ptr);
  }

  // `getClass()` is Object's method and is reserved
  public String getJsClass()
  {
    return getJsClass(this.ptr);
  }

  public List<String> getOwnPropertyNames()
  {
    return getOwnPropertyNames(this.ptr);
  }

  public List<JsValue> asList()
  {
    return asList(this.ptr);
  }

  @Override
  public String toString()
  {
    return asString(this.ptr);
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

  private final static native boolean isUndefined(long ptr);

  private final static native boolean isNull(long ptr);

  private final static native boolean isString(long ptr);

  private final static native boolean isNumber(long ptr);

  private final static native boolean isBoolean(long ptr);

  private final static native boolean isObject(long ptr);

  private final static native boolean isArray(long ptr);

  private final static native boolean isFunction(long ptr);

  private final static native String asString(long ptr);

  private final static native long asLong(long ptr);

  private final static native boolean asBoolean(long ptr);

  private final static native JsValue getProperty(long ptr, String name);

  private final static native void setProperty(long ptr, String name, long valuePtr);

  private final static native String getJsClass(long ptr);

  private final static native List<String> getOwnPropertyNames(long ptr);

  private final static native List<JsValue> asList(long ptr);

  private final static native JsValue call(long ptr, long[] paramPtrs);

  private final static native JsValue call(long ptr, long[] paramPtrs, long thisValuePtr);

  private final static native void dtor(long ptr);
}
