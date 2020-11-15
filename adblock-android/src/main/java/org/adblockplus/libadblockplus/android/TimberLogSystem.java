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

import timber.log.Timber;

public class TimberLogSystem implements LogSystem
{
  @Override
  public void logCallback(final LogLevel level, final String message, final String source)
  {
    switch (level)
    {
      default:
      case TRACE:
      case LOG:
        Timber.d( "%s: %s", source, message);
        break;
      case INFO:
        Timber.i("%s: %s", source, message);
        break;
      case WARN:
        Timber.w("%s: %s", source, message);
        break;
      case ERROR:
        Timber.e("%s: %s", source, message);
        break;
    }
  }
}
