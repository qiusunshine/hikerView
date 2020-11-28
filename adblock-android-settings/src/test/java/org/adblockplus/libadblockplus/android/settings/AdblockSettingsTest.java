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
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AdblockSettingsTest
{
  private static AdblockSettings buildModel(int subscriptionsCount, int whitelistedDomainsCount)
  {
    final AdblockSettings settings = new AdblockSettings();
    settings.setAdblockEnabled(true);
    settings.setAcceptableAdsEnabled(true);
    settings.setAllowedConnectionType(ConnectionType.WIFI);

    final List<Subscription> subscriptions = new LinkedList<>();
    for (int i = 0; i < subscriptionsCount; i++)
    {
      final Subscription subscription = new Subscription();
      subscription.title = "Title" + (i + 1);
      subscription.url = "URL" + (i + 1);
      subscriptions.add(subscription);
    }
    settings.setSubscriptions(subscriptions);

    final List<String> domains = new LinkedList<>();
    for (int i = 0; i < whitelistedDomainsCount; i++)
    {
      domains.add("http://www.domain" + (i + 1) + ".com");
    }
    settings.setWhitelistedDomains(domains);

    return settings;
  }

  private static void assertSettingsEquals(AdblockSettings expected, AdblockSettings actual)
  {
    assertEquals(expected.isAdblockEnabled(), actual.isAdblockEnabled());
    assertEquals(expected.isAcceptableAdsEnabled(), actual.isAcceptableAdsEnabled());
    assertEquals(expected.getAllowedConnectionType(), actual.getAllowedConnectionType());

    assertNotNull(actual.getSubscriptions());
    assertEquals(expected.getSubscriptions().size(), actual.getSubscriptions().size());
    for (int i = 0; i < expected.getSubscriptions().size(); i++)
    {
      assertEquals(expected.getSubscriptions().get(i).title, actual.getSubscriptions().get(i).title);
      assertEquals(expected.getSubscriptions().get(i).url, actual.getSubscriptions().get(i).url);
    }

    assertNotNull(actual.getWhitelistedDomains());
    assertEquals(expected.getWhitelistedDomains().size(), actual.getWhitelistedDomains().size());

    for (int i = 0; i < expected.getWhitelistedDomains().size(); i++)
    {
      assertEquals(expected.getWhitelistedDomains().get(i), actual.getWhitelistedDomains().get(i));
    }
  }

  @Test
  public void testAdblockEnabled()
  {
    final AdblockSettings settings = new AdblockSettings();
    settings.setAdblockEnabled(true);
    assertTrue(settings.isAdblockEnabled());

    settings.setAdblockEnabled(false);
    assertFalse(settings.isAdblockEnabled());
  }

  @Test
  public void testAcceptableAds()
  {
    final AdblockSettings settings = new AdblockSettings();
    settings.setAcceptableAdsEnabled(true);
    assertTrue(settings.isAcceptableAdsEnabled());

    settings.setAcceptableAdsEnabled(false);
    assertFalse(settings.isAcceptableAdsEnabled());
  }

  @Test
  public void testAllowedConnectionType()
  {
    final AdblockSettings settings = new AdblockSettings();
    for (ConnectionType eachConnectionType : ConnectionType.values())
    {
      settings.setAllowedConnectionType(eachConnectionType);
      assertEquals(eachConnectionType, settings.getAllowedConnectionType());
    }
  }

  @Test
  public void testSubscriptions()
  {
    for (int i = 0; i < 3; i++)
    {
      final AdblockSettings settings = buildModel(i, 1);
      assertEquals(i, settings.getSubscriptions().size());
    }
  }

  @Test
  public void testWhitelistedDomains()
  {
    for (int i = 0; i < 3; i++)
    {
      final AdblockSettings settings = buildModel(1, i);
      assertEquals(i, settings.getWhitelistedDomains().size());
    }
  }

  @Test
  public void testSerializable() throws IOException, ClassNotFoundException
  {
    final AdblockSettings savedSettings = buildModel(2, 3);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(savedSettings);

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);
    AdblockSettings loadedSettings = (AdblockSettings) ois.readObject();

    assertSettingsEquals(savedSettings, loadedSettings);
  }
}
