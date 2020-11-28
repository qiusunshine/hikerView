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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

/**
 * Whitelisted domains adblock fragment.
 * Use the {@link WhitelistedDomainsSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WhitelistedDomainsSettingsFragment
        extends BaseSettingsFragment<WhitelistedDomainsSettingsFragment.Listener> {
    private EditText domain;
    private ImageView addDomainButton;
    private ListView listView;
    private Adapter adapter;

    /**
     * Listener with additional `isValidDomain` method
     */
    public interface Listener extends BaseSettingsFragment.Listener {
        boolean isValidDomain(WhitelistedDomainsSettingsFragment fragment,
                              String domain, AdblockSettings settings);
    }

    public WhitelistedDomainsSettingsFragment() {
        // required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static WhitelistedDomainsSettingsFragment newInstance() {
        return new WhitelistedDomainsSettingsFragment();
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        listener = castOrThrow(activity, Listener.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_adblock_whitelisted_domains_settings,
                container,
                false);

        bindControls(rootView);

        initControls();
        return rootView;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String key) {
        // nothing
    }

    private void bindControls(View rootView) {
        domain = rootView.findViewById(R.id.fragment_adblock_wl_add_label);
        addDomainButton = rootView.findViewById(R.id.fragment_adblock_wl_add_button);
        listView = rootView.findViewById(R.id.fragment_adblock_wl_listview);
    }

    // Holder for listview items
    private class Holder {
        TextView domain;
        ImageView removeButton;

        Holder(View rootView) {
            domain = rootView.findViewById(R.id.fragment_adblock_wl_item_title);
            removeButton = rootView.findViewById(R.id.fragment_adblock_wl_item_remove);
        }
    }

    private View.OnClickListener removeDomainClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // update and save settings
            int position = (Integer) v.getTag();
            String removeDomain = settings.getWhitelistedDomains().get(position);
            Timber.w("Removing domain: %s", removeDomain);
            settings.getWhitelistedDomains().remove(position);
            provider.getAdblockSettingsStorage().save(settings);

            // apply settings
            provider.getAdblockEngine().setWhitelistedDomains(settings.getWhitelistedDomains());

            // signal event
            listener.onAdblockSettingsChanged(WhitelistedDomainsSettingsFragment.this);

            // update UI
            adapter.notifyDataSetChanged();
        }
    };

    // Adapter
    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return settings.getWhitelistedDomains() != null
                    ? settings.getWhitelistedDomains().size()
                    : 0;
        }

        @Override
        public Object getItem(int position) {
            return settings.getWhitelistedDomains().get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                convertView = inflater.inflate(R.layout.fragment_adblock_whitelisted_domain_item,
                        parent, false);
                convertView.setTag(new Holder(convertView));
            }

            String domain = (String) getItem(position);

            Holder holder = (Holder) convertView.getTag();
            holder.domain.setText(domain);

            holder.removeButton.setOnClickListener(removeDomainClickListener);
            holder.removeButton.setTag(Integer.valueOf(position));

            return convertView;
        }
    }

    private void initControls() {
        addDomainButton.setOnClickListener(v -> {
            String preparedDomain = prepareDomain(domain.getText().toString());
            if (preparedDomain == null || preparedDomain.length() < 1) {
                ToastMgr.shortCenter(getContext(), "域名不能为空");
                return;
            }
            if (listener.isValidDomain(
                    WhitelistedDomainsSettingsFragment.this,
                    preparedDomain,
                    settings)) {
                addDomain(preparedDomain);
            } else {
                Timber.w("Domain " + preparedDomain + " is not valid");
            }
        });

        adapter = new Adapter();
        listView.setAdapter(adapter);
    }

    private String prepareDomain(String domain) {
        return Utils.getDom(domain);
    }

    public void addDomain(String newDomain) {
        Timber.d("New domain added: " + newDomain);

        List<String> whitelistedDomains = settings.getWhitelistedDomains();
        if (whitelistedDomains == null) {
            whitelistedDomains = new LinkedList<>();
            settings.setWhitelistedDomains(whitelistedDomains);
        }

        // update and save settings
        whitelistedDomains.add(newDomain);
        provider.getAdblockSettingsStorage().save(settings);

        // apply settings
        provider.getAdblockEngine().setWhitelistedDomains(whitelistedDomains);

        // signal event
        listener.onAdblockSettingsChanged(WhitelistedDomainsSettingsFragment.this);

        // update UI
        adapter.notifyDataSetChanged();
        domain.getText().clear();
        domain.clearFocus();
    }
}
