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

public final class Filter extends JsValue
{
  static
  {
    System.loadLibrary(BuildConfig.nativeLibraryName);
    registerNatives();
  }

  private Filter(final long pointer)
  {
    super(pointer);
  }

  public Type getType()
  {
    return getType(this.ptr);
  }

  public boolean isListed()
  {
    return isListed(this.ptr);
  }

  public void addToList()
  {
    addToList(this.ptr);
  }

  public void removeFromList()
  {
    removeFromList(this.ptr);
  }

  @Override
  public int hashCode()
  {
    return (int)(this.ptr >> 32) * (int)this.ptr;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (!(o instanceof Filter))
    {
      return false;
    }

    return operatorEquals(this.ptr, ((Filter)o).ptr);
  }

  public enum Type
  {
    BLOCKING, EXCEPTION, ELEMHIDE, ELEMHIDE_EXCEPTION, ELEMHIDE_EMULATION,
    COMMENT, INVALID;
  }

  private final static native void registerNatives();

  private final static native Type getType(long ptr);

  private final static native boolean isListed(long ptr);

  private final static native void addToList(long ptr);

  private final static native void removeFromList(long ptr);

  private final static native boolean operatorEquals(long ptr, long other);
}
