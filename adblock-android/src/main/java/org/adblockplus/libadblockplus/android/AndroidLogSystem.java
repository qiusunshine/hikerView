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

package org.adblockplus.libadblockplus.android;

import org.adblockplus.libadblockplus.LogSystem;

import android.util.Log;

public class AndroidLogSystem implements LogSystem
{
  private static int abpLogLevelToAndroid(final LogLevel level)
  {
    switch (level)
    {
      default:
      case TRACE:
      case LOG:
        return Log.VERBOSE;
      case INFO:
        return Log.INFO;
      case WARN:
        return Log.WARN;
      case ERROR:
        return Log.ERROR;
    }
  }

  @Override
  public void logCallback(final LogLevel level, final String message, final String source)
  {
    Log.println(abpLogLevelToAndroid(level), source, message);
  }
}
