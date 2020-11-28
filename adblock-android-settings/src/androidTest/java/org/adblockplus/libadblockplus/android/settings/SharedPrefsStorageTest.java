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

import android.content.Context;
import android.content.SharedPreferences;

import org.adblockplus.libadblockplus.android.ConnectionType;
import org.adblockplus.libadblockplus.android.Subscription;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import androidx.test.core.app.ApplicationProvider;

import timber.log.Timber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SharedPrefsStorageTest
{
  protected static final String NAME = "prefs";

  protected SharedPreferences prefs;
  protected SharedPrefsStorage storage;

  @BeforeClass
  public static void beforeClass()
  {
    if (BuildConfig.DEBUG)
    {
      Timber.plant(new Timber.DebugTree());
    }
  }

  public static AdblockSettings buildModel(int subscriptionsCount, int whitelistedDomainsCount)
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

  public static void assertSettingsEquals(AdblockSettings expected, AdblockSettings actual)
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

  protected void _testAdblockEnabled()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);
    storage.save(savedSettings);

    AdblockSettings loadedSettings = storage.load();
    assertEquals(savedSettings.isAdblockEnabled(), loadedSettings.isAdblockEnabled());

    savedSettings.setAdblockEnabled(false);
    storage.save(savedSettings);

    loadedSettings = storage.load();
    assertEquals(savedSettings.isAdblockEnabled(), loadedSettings.isAdblockEnabled());
  }

  @Before
  public void setUp()
  {
    prefs = ApplicationProvider.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    prefs.edit().clear().commit();

    storage = new SharedPrefsStorage(prefs);
    storage.setCommit(true);
  }

  @Test
  public void testNeverSaved()
  {
    final AdblockSettings settings = storage.load();
    assertNull(settings);
  }

  @Test
  public void testAdblockEnabled()
  {
    storage.setCommit(true);
    _testAdblockEnabled();
  }

  @Test
  public void testAdblockEnabled_Apply()
  {
    storage.setCommit(false); // commit is sync (for `true`), apply is async (for `false`)
    _testAdblockEnabled();
  }

  @Test
  public void testAcceptableAdsEnabled()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAcceptableAdsEnabled(true);
    storage.save(savedSettings);

    AdblockSettings loadedSettings = storage.load();
    assertEquals(savedSettings.isAcceptableAdsEnabled(), loadedSettings.isAcceptableAdsEnabled());

    savedSettings.setAcceptableAdsEnabled(false);
    storage.save(savedSettings);

    loadedSettings = storage.load();
    assertEquals(savedSettings.isAcceptableAdsEnabled(), loadedSettings.isAcceptableAdsEnabled());
  }

  @Test
  public void testSubscriptions_Null()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);
    assertNull(savedSettings.getSubscriptions());
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNull(loadedSettings.getSubscriptions());
  }

  @Test
  public void testSubscriptions_Empty()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);
    savedSettings.setSubscriptions(Collections.<Subscription>emptyList());
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNotNull(loadedSettings.getSubscriptions());
    assertEquals(0, loadedSettings.getSubscriptions().size());
  }

  @Test
  public void testSubscriptions_OneValue()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);

    final List<Subscription> subscriptions = new LinkedList<>();
    final Subscription savedSubscription = new Subscription();
    savedSubscription.title = "Title";
    savedSubscription.url = "URL";
    subscriptions.add(savedSubscription);

    savedSettings.setSubscriptions(subscriptions);
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNotNull(loadedSettings.getSubscriptions());
    assertEquals(1, loadedSettings.getSubscriptions().size());

    final Subscription loadedSubscription = loadedSettings.getSubscriptions().get(0);
    assertEquals(savedSubscription.title, loadedSubscription.title);
    assertEquals(savedSubscription.url, loadedSubscription.url);

    // 'author', 'homepage', 'specialization', 'prefixes' settings are not saved by SharedPrefsStorage
  }

  @Test
  public void testSubscriptions_OneValueNonEnglishTitle()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);

    final List<Subscription> subscriptions = new LinkedList<>();
    final Subscription savedSubscription = new Subscription();
    savedSubscription.title = "Заголовок"; // non-English characters
    savedSubscription.url = "URL";
    subscriptions.add(savedSubscription);

    savedSettings.setSubscriptions(subscriptions);
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNotNull(loadedSettings.getSubscriptions());
    assertEquals(1, loadedSettings.getSubscriptions().size());

    final Subscription loadedSubscription = loadedSettings.getSubscriptions().get(0);
    assertEquals(savedSubscription.title, loadedSubscription.title);
    assertEquals(savedSubscription.url, loadedSubscription.url);

    // 'author', 'homepage', 'specialization', 'prefixes' settings are not saved by SharedPrefsStorage
  }

  @Test
  public void testSubscriptions_OneValueNonEnglishUrl()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);

    final List<Subscription> subscriptions = new LinkedList<>();
    final Subscription savedSubscription = new Subscription();
    savedSubscription.title = "Title";
    savedSubscription.url = "http://почта.рф";  // non-English characters
    subscriptions.add(savedSubscription);

    savedSettings.setSubscriptions(subscriptions);
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNotNull(loadedSettings.getSubscriptions());
    assertEquals(1, loadedSettings.getSubscriptions().size());

    final Subscription loadedSubscription = loadedSettings.getSubscriptions().get(0);
    assertEquals(savedSubscription.title, loadedSubscription.title);
    assertEquals(savedSubscription.url, loadedSubscription.url);

    // 'author', 'homepage', 'specialization', 'prefixes' settings are not saved by SharedPrefsStorage
  }

  @Test
  public void testSubscriptions_MultipleValues()
  {
    AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);

    final List<Subscription> subscriptions = new LinkedList<>();
    final Subscription savedSubscription1 = new Subscription();
    savedSubscription1.title = "Title1";
    savedSubscription1.url = "URL1";
    subscriptions.add(savedSubscription1);

    final Subscription savedSubscription2 = new Subscription();
    savedSubscription2.title = "Title2";
    savedSubscription2.url = "URL2";
    subscriptions.add(savedSubscription2);

    savedSettings.setSubscriptions(subscriptions);
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNotNull(loadedSettings.getSubscriptions());
    assertEquals(2, loadedSettings.getSubscriptions().size());

    final Subscription loadedSubscription1 = loadedSettings.getSubscriptions().get(0);
    assertEquals(savedSubscription1.title, loadedSubscription1.title);
    assertEquals(savedSubscription1.url, loadedSubscription1.url);

    final Subscription loadedSubscription2 = loadedSettings.getSubscriptions().get(1);
    assertEquals(savedSubscription2.title, loadedSubscription2.title);
    assertEquals(savedSubscription2.url, loadedSubscription2.url);
  }

  @Test
  public void testWhitelistedDomains_Null()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);
    assertNull(savedSettings.getWhitelistedDomains());
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNull(loadedSettings.getWhitelistedDomains());
  }

  @Test
  public void testWhitelistedDomains_Empty()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);
    savedSettings.setWhitelistedDomains(Collections.<String>emptyList());
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNotNull(loadedSettings.getWhitelistedDomains());
    assertEquals(0, loadedSettings.getWhitelistedDomains().size());
  }

  @Test
  public void testWhitelistedDomains_SingleValue()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);

    final List<String> whitelistedDomains = new LinkedList<>();
    whitelistedDomains.add("http://www.domain1.com");

    savedSettings.setWhitelistedDomains(whitelistedDomains);
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNotNull(loadedSettings.getWhitelistedDomains());
    assertEquals(1, loadedSettings.getWhitelistedDomains().size());

    assertEquals(
        savedSettings.getWhitelistedDomains().get(0),
        loadedSettings.getWhitelistedDomains().get(0));
  }

  @Test
  public void testWhitelistedDomains_SingleValueNonEnglish()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);

    final List<String> whitelistedDomains = new LinkedList<>();
    whitelistedDomains.add("http://почта.рф");

    savedSettings.setWhitelistedDomains(whitelistedDomains);
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNotNull(loadedSettings.getWhitelistedDomains());
    assertEquals(1, loadedSettings.getWhitelistedDomains().size());

    assertEquals(
        savedSettings.getWhitelistedDomains().get(0),
        loadedSettings.getWhitelistedDomains().get(0));
  }

  @Test
  public void testWhitelistedDomains_MultipleValues()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAdblockEnabled(true);

    final List<String> whitelistedDomains = new LinkedList<>();
    whitelistedDomains.add("http://www.domain1.com");
    whitelistedDomains.add("http://www.domain2.com");

    savedSettings.setWhitelistedDomains(whitelistedDomains);
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNotNull(loadedSettings.getWhitelistedDomains());
    assertEquals(2, loadedSettings.getWhitelistedDomains().size());

    assertEquals(
        savedSettings.getWhitelistedDomains().get(0),
        loadedSettings.getWhitelistedDomains().get(0));

    assertEquals(
        savedSettings.getWhitelistedDomains().get(1),
        loadedSettings.getWhitelistedDomains().get(1));
  }

  @Test
  public void testAllowedConnectionType_Null()
  {
    final AdblockSettings savedSettings = new AdblockSettings();
    savedSettings.setAllowedConnectionType(null);
    assertNull(savedSettings.getAllowedConnectionType());
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();
    assertNull(loadedSettings.getAllowedConnectionType());
  }

  @Test
  public void testAllowedConnectionType()
  {
    final AdblockSettings savedSettings = new AdblockSettings();

    for (ConnectionType eachConnectionType : ConnectionType.values())
    {
      savedSettings.setAllowedConnectionType(eachConnectionType);
      storage.save(savedSettings);

      final AdblockSettings loadedSettings = storage.load();
      assertEquals(savedSettings.getAllowedConnectionType(), loadedSettings.getAllowedConnectionType());
    }
  }

  @Test
  public void testFullModel()
  {
    final AdblockSettings savedSettings = buildModel(2, 1);
    storage.save(savedSettings);

    final AdblockSettings loadedSettings = storage.load();

    assertSettingsEquals(savedSettings, loadedSettings);
  }

  @After
  public void tearDown()
  {
    prefs.edit().clear().commit();
  }
}
