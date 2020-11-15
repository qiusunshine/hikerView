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

import java.util.List;

public final class JsEngine
{
  protected final long ptr;

  static
  {
    System.loadLibrary(BuildConfig.nativeLibraryName);
    registerNatives();
  }

  JsEngine(final long ptr)
  {
    this.ptr = ptr;
  }

  public void setEventCallback(final String eventName, final EventCallback callback)
  {
    setEventCallback(this.ptr, eventName, callback.ptr);
  }

  public void removeEventCallback(final String eventName)
  {
    removeEventCallback(this.ptr, eventName);
  }

  public JsValue evaluate(final String source, final String filename)
  {
    return evaluate(this.ptr, source, filename);
  }

  public JsValue evaluate(final String source)
  {
    return evaluate(this.ptr, source, "");
  }

  public void setGlobalProperty(final String property, final JsValue value)
  {
    setGlobalProperty(this.ptr, property, value.ptr);
  }

  public void triggerEvent(final String eventName, final List<JsValue> params)
  {
    final long[] args = new long[params.size()];

    for (int i = 0; i < args.length; i++)
    {
      args[i] = params.get(i).ptr;
    }

    triggerEvent(this.ptr, eventName, args);
  }

  public void triggerEvent(final String eventName)
  {
    triggerEvent(this.ptr, eventName, null);
  }

  public JsValue newValue(final long value)
  {
    return newValue(this.ptr, value);
  }

  public JsValue newValue(final boolean value)
  {
    return newValue(this.ptr, value);
  }

  public JsValue newValue(final String value)
  {
    return newValue(this.ptr, value);
  }

  private final static native void registerNatives();

  private final static native void setEventCallback(long ptr, String eventName, long callback);

  private final static native void removeEventCallback(long ptr, String eventName);

  private final static native JsValue evaluate(long ptr, String source, String filename);

  private final static native void triggerEvent(long ptr, String eventName, long[] args);

  private final static native JsValue newValue(long ptr, long value);

  private final static native JsValue newValue(long ptr, boolean value);

  private final static native JsValue newValue(long ptr, String value);

  private final static native void setGlobalProperty(long ptr, String property, long valuePtr);
}
