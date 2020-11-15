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

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import timber.log.Timber;

import org.adblockplus.libadblockplus.IsAllowedConnectionCallback;

public class IsAllowedConnectionCallbackImpl implements IsAllowedConnectionCallback
{
  private ConnectivityManager manager;

  public IsAllowedConnectionCallbackImpl(ConnectivityManager manager)
  {
    super();
    this.manager = manager;
  }

  @Override
  public boolean isConnectionAllowed(String connection)
  {
    Timber.d("Checking connection: %s", connection);

    if (connection == null)
    {
      // required connection type is not specified - any works
      return true;
    }

    NetworkInfo info = manager.getActiveNetworkInfo();
    if (info == null || !info.isConnected())
    {
      // not connected
      return false;
    }

    ConnectionType connectionType = ConnectionType.findByValue(connection);
    if (connectionType == null)
    {
      Timber.e("Unknown connection type: %s", connection);
      return false;
    }

    if (!connectionType.isRequiredConnection(manager))
    {
      Timber.w("Current connection type is not allowed for web requests");
      return false;
    }

    return true;
  }
}
