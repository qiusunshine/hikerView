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

public final class Subscription extends JsValue
{
  static
  {
    System.loadLibrary(BuildConfig.nativeLibraryName);
    registerNatives();
  }

  private Subscription(final long ptr)
  {
    super(ptr);
  }

  public boolean isDisabled()
  {
    return isDisabled(this.ptr);
  }

  public void setDisabled(boolean disabled)
  {
    setDisabled(this.ptr, disabled);
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

  public void updateFilters()
  {
    updateFilters(this.ptr);
  }

  public boolean isUpdating()
  {
    return isUpdating(this.ptr);
  }

  public boolean isAcceptableAds()
  {
    return isAcceptableAds(this.ptr);
  }

  @Override
  public int hashCode()
  {
    return (int)(this.ptr >> 32) * (int)this.ptr;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (!(o instanceof Subscription))
    {
      return false;
    }

    return operatorEquals(this.ptr, ((Subscription)o).ptr);
  }

  private final static native void registerNatives();

  private final static native boolean isDisabled(long ptr);

  private final static native void setDisabled(long ptr, boolean disabled);

  private final static native boolean isListed(long ptr);

  private final static native void addToList(long ptr);

  private final static native void removeFromList(long ptr);

  private final static native void updateFilters(long ptr);

  private final static native boolean isUpdating(long ptr);

  private final static native boolean operatorEquals(long ptr, long other);

  private final static native boolean isAcceptableAds(long ptr);
}
