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

public class AdblockPlusException extends RuntimeException
{
  private static final long serialVersionUID = -8127654134450836743L;

  public AdblockPlusException(final String message)
  {
    super(message);
  }

  public AdblockPlusException(final String message, final Throwable throwable)
  {
    super(message, throwable);
  }

  public AdblockPlusException(final Throwable throwable)
  {
    super(throwable);
  }
}
