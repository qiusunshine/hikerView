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

public class Notification extends JsValue
{
  static
  {
    System.loadLibrary(BuildConfig.nativeLibraryName);
    registerNatives();
  }

  private Notification(final long ptr)
  {
    super(ptr);
  }

  public enum Type
  {
    INFORMATION,
    NEWTAB,
    RELENTLESS,
    CRITICAL,
    INVALID
  }

  public String getMessageString()
  {
    return getMessageString(this.ptr);
  }

  public String getTitle()
  {
    return getTitle(this.ptr);
  }

  public Type getType()
  {
    return getType(this.ptr);
  }

  public void markAsShown()
  {
    markAsShown(this.ptr);
  }

  public List<String> getLinks()
  {
    return getLinks(this.ptr);
  }

  @Override
  public String toString()
  {
    return this.getTitle() + " - " + this.getMessageString();
  }

  private final static native void registerNatives();

  private final static native String getMessageString(long ptr);

  private final static native String getTitle(long ptr);

  private final static native Type getType(long ptr);

  private final static native void markAsShown(long ptr);

  private final static native List<String> getLinks(long ptr);
}
