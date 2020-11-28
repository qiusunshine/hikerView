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

import android.content.SharedPreferences;

import org.adblockplus.libadblockplus.android.ConnectionType;
import org.adblockplus.libadblockplus.android.Subscription;

import java.util.LinkedList;
import java.util.List;

/**
 * Settings storage implementation in Shared Preferences
 */
public class SharedPrefsStorage extends AdblockSettingsStorage
{
  private static final String SETTINGS_ENABLED_KEY = "enabled";
  private static final String SETTINGS_AA_ENABLED_KEY = "aa_enabled";
  private static final String SETTINGS_SUBSCRIPTIONS_KEY = "subscriptions";
  private static final String SETTINGS_SUBSCRIPTION_KEY = "subscription";
  private static final String SETTINGS_SUBSCRIPTION_URL_KEY = "url";
  private static final String SETTINGS_SUBSCRIPTION_PREFIXES_KEY = "prefixes";
  private static final String SETTINGS_SUBSCRIPTION_TITLE_KEY = "title";
  private static final String SETTINGS_WL_DOMAINS_KEY = "whitelisted_domains";
  private static final String SETTINGS_WL_DOMAIN_KEY = "domain";
  private static final String SETTINGS_ALLOWED_CONNECTION_TYPE_KEY = "allowed_connection_type";

  private SharedPreferences prefs;
  private boolean commit = true;

  public SharedPrefsStorage(SharedPreferences prefs)
  {
    this.prefs = prefs;
  }

  public boolean isCommit()
  {
    return commit;
  }

  /**
   * Do commit the changes in save() before return
   *
   * @param commit `true` to commit, `false`
   */
  public void setCommit(boolean commit)
  {
    this.commit = commit;
  }

  @Override
  public AdblockSettings load()
  {
    if (!prefs.contains(SETTINGS_ENABLED_KEY))
    {
      // settings were not saved yet
      return null;
    }

    AdblockSettings settings = new AdblockSettings();
    settings.setAdblockEnabled(prefs.getBoolean(SETTINGS_ENABLED_KEY, true));
    settings.setAcceptableAdsEnabled(prefs.getBoolean(SETTINGS_AA_ENABLED_KEY, true));
    String connectionType = prefs.getString(SETTINGS_ALLOWED_CONNECTION_TYPE_KEY, null);
    settings.setAllowedConnectionType(ConnectionType.findByValue(connectionType));

    loadSubscriptions(settings);
    loadWhitelistedDomains(settings);

    return settings;
  }

  private void loadWhitelistedDomains(AdblockSettings settings)
  {
    if (prefs.contains(SETTINGS_WL_DOMAINS_KEY))
    {
      // count
      int whitelistedDomainsCount = prefs.getInt(SETTINGS_WL_DOMAINS_KEY, 0);

      // each domain
      List<String> whitelistedDomains = new LinkedList<>();
      for (int i = 0; i < whitelistedDomainsCount; i++)
      {
        String whitelistedDomain = prefs.getString(getArrayItemKey(i, SETTINGS_WL_DOMAIN_KEY), "");
        whitelistedDomains.add(whitelistedDomain);
      }
      settings.setWhitelistedDomains(whitelistedDomains);
    }
  }

  private void loadSubscriptions(AdblockSettings settings)
  {
    if (prefs.contains(SETTINGS_SUBSCRIPTIONS_KEY))
    {
      // count
      int subscriptionsCount = prefs.getInt(SETTINGS_SUBSCRIPTIONS_KEY, 0);

      // each subscription
      List<Subscription> subscriptions = new LinkedList<>();
      for (int i = 0; i < subscriptionsCount; i++)
      {
        Subscription subscription = new Subscription();
        subscription.title = prefs.getString(getSubscriptionTitleKey(i), "");
        subscription.url = prefs.getString(getSubscriptionURLKey(i), "");
        subscription.prefixes = prefs.getString(getSubscriptionPrefixesKey(i), "");
        subscriptions.add(subscription);
      }
      settings.setSubscriptions(subscriptions);
    }
  }

  private String getArrayItemKey(int index, String entity)
  {
    // f.e. "domain0"
    return entity + index;
  }

  private String getArrayItemKey(int index, String entity, String field)
  {
    // f.e. `subscription0.field`
    return getArrayItemKey(index, entity) + "." + field;
  }

  private String getSubscriptionTitleKey(int index)
  {
    return getArrayItemKey(index, SETTINGS_SUBSCRIPTION_KEY, SETTINGS_SUBSCRIPTION_TITLE_KEY);
  }

  private String getSubscriptionURLKey(int index)
  {
    return getArrayItemKey(index, SETTINGS_SUBSCRIPTION_KEY, SETTINGS_SUBSCRIPTION_URL_KEY);
  }

  private String getSubscriptionPrefixesKey(int index)
  {
    return getArrayItemKey(index, SETTINGS_SUBSCRIPTION_KEY, SETTINGS_SUBSCRIPTION_PREFIXES_KEY);
  }

  @Override
  public void save(AdblockSettings settings)
  {
    SharedPreferences.Editor editor = prefs
      .edit()
      .clear()
      .putBoolean(SETTINGS_ENABLED_KEY, settings.isAdblockEnabled())
      .putBoolean(SETTINGS_AA_ENABLED_KEY, settings.isAcceptableAdsEnabled());

    if (settings.getAllowedConnectionType() != null)
    {
      editor.putString(SETTINGS_ALLOWED_CONNECTION_TYPE_KEY, settings.getAllowedConnectionType().getValue());
    }

    saveSubscriptions(settings, editor);
    saveWhitelistedDomains(settings, editor);

    if (commit)
    {
      editor.commit();
    }
    else
    {
      // faster but not finished most likely before return
      editor.apply();
    }
  }

  private void saveWhitelistedDomains(AdblockSettings settings, SharedPreferences.Editor editor)
  {
    if (settings.getWhitelistedDomains() != null)
    {
      // count
      editor.putInt(SETTINGS_WL_DOMAINS_KEY, settings.getWhitelistedDomains().size());

      // each domain
      for (int i = 0; i < settings.getWhitelistedDomains().size(); i++)
      {
        String eachDomain = settings.getWhitelistedDomains().get(i);
        editor.putString(getArrayItemKey(i, SETTINGS_WL_DOMAIN_KEY), eachDomain);
      }
    }
  }

  private void saveSubscriptions(AdblockSettings settings, SharedPreferences.Editor editor)
  {
    if (settings.getSubscriptions() != null)
    {
      // count
      editor.putInt(SETTINGS_SUBSCRIPTIONS_KEY, settings.getSubscriptions().size());

      // each subscription
      for (int i = 0; i < settings.getSubscriptions().size(); i++)
      {
        Subscription eachSubscription = settings.getSubscriptions().get(i);

        // warning: saving `title`, `url` and `prefixes` fields only
        editor.putString(getSubscriptionTitleKey(i), eachSubscription.title);
        editor.putString(getSubscriptionURLKey(i), eachSubscription.url);
        editor.putString(getSubscriptionPrefixesKey(i), eachSubscription.prefixes);
      }
    }
  }
}
