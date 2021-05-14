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

package com.example.hikerview.ui.adblock;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import org.adblockplus.libadblockplus.android.AdblockEngine;
import org.adblockplus.libadblockplus.android.settings.AdblockHelper;
import org.adblockplus.libadblockplus.android.settings.AdblockSettings;
import org.adblockplus.libadblockplus.android.settings.AdblockSettingsStorage;
import org.adblockplus.libadblockplus.android.settings.BaseSettingsFragment;
import org.adblockplus.libadblockplus.android.settings.GeneralSettingsFragment;
import org.adblockplus.libadblockplus.android.settings.WhitelistedDomainsSettingsFragment;

import timber.log.Timber;

public class AdSettingsActivity
        extends AppCompatActivity
        implements
        BaseSettingsFragment.Provider,
        GeneralSettingsFragment.Listener,
        WhitelistedDomainsSettingsFragment.Listener {

    private BaseSettingsFragment generalSettingsFragment, whitelistedDomainsSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // retaining AdblockEngine asynchronously
//        AdblockHelper.get().getProvider().retain(true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setTitle("Adblock Plus 数据载入中");
        super.onCreate(savedInstanceState);

        insertGeneralFragment();
    }

    private void insertGeneralFragment() {
        if (generalSettingsFragment == null) {
            generalSettingsFragment = GeneralSettingsFragment.newInstance();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, generalSettingsFragment)
                .commit();
    }

    private void insertWhitelistedFragment() {
        if (whitelistedDomainsSettingsFragment == null) {
            whitelistedDomainsSettingsFragment = WhitelistedDomainsSettingsFragment.newInstance();
        }
        whitelistedDomainsSettingsFragment.setSettings(generalSettingsFragment.getSettings());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, whitelistedDomainsSettingsFragment)
                .addToBackStack(WhitelistedDomainsSettingsFragment.class.getSimpleName())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    // provider

    @Override
    public AdblockEngine getAdblockEngine() {
        // if it's retained asynchronously we have to wait until it's ready
        AdblockHelper.get().getProvider().waitForReady();
        return AdblockHelper.get().getProvider().getEngine();
    }

    @Override
    public AdblockSettingsStorage getAdblockSettingsStorage() {
        return AdblockHelper.get().getStorage();
    }

    // listener

    @Override
    public void onAdblockSettingsChanged(BaseSettingsFragment fragment) {
        Timber.d("AdblockHelper setting changed:\n%s", fragment.getSettings().toString());
    }

    @Override
    public void onWhitelistedDomainsClicked(GeneralSettingsFragment fragment) {
        insertWhitelistedFragment();
    }

    @Override
    public boolean isValidDomain(WhitelistedDomainsSettingsFragment fragment,
                                 String domain,
                                 AdblockSettings settings) {
        // show error here if domain is invalid
        return domain != null && domain.length() > 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        AdblockHelper.get().getProvider().release();
    }
}
