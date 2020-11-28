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

package org.adblockplus.libadblockplus.android.settings;

import org.adblockplus.libadblockplus.android.ConnectionType;
import org.adblockplus.libadblockplus.android.Subscription;

import java.io.Serializable;
import java.util.List;

/**
 * Adblock settings
 */
public class AdblockSettings implements Serializable
{
  private volatile boolean adblockEnabled;
  private volatile boolean acceptableAdsEnabled;
  private List<Subscription> subscriptions;
  private List<String> whitelistedDomains;
  private ConnectionType allowedConnectionType;

  public boolean isAdblockEnabled()
  {
    return adblockEnabled;
  }

  public void setAdblockEnabled(boolean adblockEnabled)
  {
    this.adblockEnabled = adblockEnabled;
  }

  public boolean isAcceptableAdsEnabled()
  {
    return acceptableAdsEnabled;
  }

  public void setAcceptableAdsEnabled(boolean acceptableAdsEnabled)
  {
    this.acceptableAdsEnabled = acceptableAdsEnabled;
  }

  public List<Subscription> getSubscriptions()
  {
    return subscriptions;
  }

  public void setSubscriptions(List<Subscription> subscriptions)
  {
    this.subscriptions = subscriptions;
  }

  public List<String> getWhitelistedDomains()
  {
    return whitelistedDomains;
  }

  public void setWhitelistedDomains(List<String> whitelistedDomains)
  {
    this.whitelistedDomains = whitelistedDomains;
  }

  public ConnectionType getAllowedConnectionType()
  {
    return allowedConnectionType;
  }

  public void setAllowedConnectionType(ConnectionType allowedConnectionType)
  {
    this.allowedConnectionType = allowedConnectionType;
  }

  @Override
  public String toString()
  {
    return "AdblockSettings{" +
      "adblockEnabled=" + adblockEnabled +
      ", acceptableAdsEnabled=" + acceptableAdsEnabled +
      ", subscriptions:" + (subscriptions != null ? subscriptions.size() : 0) +
      ", whitelistedDomains:" + (whitelistedDomains != null ? whitelistedDomains.size() : 0) +
      ", allowedConnectionType=" + (allowedConnectionType != null ? allowedConnectionType.getValue() : "null") +
      '}';
  }
}
