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
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;
import com.lxj.xpopup.interfaces.OnConfirmListener;

import org.adblockplus.libadblockplus.FilterEngine;
import org.adblockplus.libadblockplus.JsValue;
import org.adblockplus.libadblockplus.Platform;
import org.adblockplus.libadblockplus.android.AdblockEngine;
import org.adblockplus.libadblockplus.android.ConnectionType;
import org.adblockplus.libadblockplus.android.Subscription;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

/**
 * General Adblock settings fragment.
 * Use the {@link GeneralSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GeneralSettingsFragment
        extends BaseSettingsFragment<GeneralSettingsFragment.Listener>
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "GeneralSettingsFragment";
    private String SETTINGS_ENABLED_KEY;
    private String SETTINGS_FILTER_LISTS_KEY;
    private String SETTINGS_FILTER_LISTS_ADD_KEY;
    private String SETTINGS_FILTER_LISTS_CLEAR_KEY;
    private String SETTINGS_FILTER_MY_KEY;
    private String SETTINGS_AA_ENABLED_KEY;
    private String SETTINGS_WL_DOMAINS_KEY;
    private String SETTINGS_ALLOWED_CONNECTION_TYPE_KEY;

    private SwitchPreferenceCompat adblockEnabled;
    private MultiSelectListPreference filterLists;
    private Preference filterListsAdd, filterMy, filterClear;
    private SwitchPreferenceCompat acceptableAdsEnabled;
    private Preference whitelistedDomains;
    private ListPreference allowedConnectionType;
    private MySubscription[] availableSubscriptions;
    private LoadingPopupView loadingPopupView;
    private static final String NOT_SUB_ABP_URLS = "notSubAbpUrls";
    private Set<String> selectedFilterUrls = new HashSet<>();
    private List<MySubscription> recommendSubs = new ArrayList<>();
    private List<MySubscription> mySubs = new ArrayList<>();

    /**
     * Listener with additional `onWhitelistedDomainsClicked` event
     */
    public interface Listener extends BaseSettingsFragment.Listener {
        void onWhitelistedDomainsClicked(GeneralSettingsFragment fragment);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment GeneralSettingsFragment.
     */
    public static GeneralSettingsFragment newInstance() {
        return new GeneralSettingsFragment();
    }

    public GeneralSettingsFragment() {
        // required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        listener = castOrThrow(activity, Listener.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Issue DP-212: In case GeneralSettingsFragment was destroyed and recreated
        // (app minimized and restored scenario) and some of it's child views were
        // displayed before app was minimized, app can crash after restoring because
        // of not initialized preferences because child views are being displayed before
        // onResume() is called.
        // So we call also here initPreferences() to fix that allowing to be called
        // twice when  GeneralSettingsFragment is created.
        new Thread(() -> {
            try {
                try {
                    if (!AdblockHelper.get().isInit() && getContext() != null) {
                        // init Adblock
                        String basePath = getContext().getDir(AdblockEngine.BASE_PATH_DIRECTORY, Context.MODE_PRIVATE).getAbsolutePath();
                        AdblockHelper.get()
                                .init(getContext(), basePath, true, AdblockHelper.PREFERENCE_NAME)
                                .setDisabledByDefault();
                        AdblockHelper.get().getProvider().retain(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                AdblockHelper.get().getProvider().waitForReady();
                availableSubscriptions = getRecommendedSubscriptions();
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                loadSettings();
                if (getActivity() != null) {
                    if (getActivity().isFinishing()) {
                        return;
                    }
                    getActivity().runOnUiThread(() -> {
                        if (getActivity() == null || getActivity().isFinishing()) {
                            return;
                        }
                        getActivity().setTitle("Adblock Plus 订阅");
                        initPreferences();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String key) {
        readKeys();
        addPreferencesFromResource(R.xml.preference_adblock_general);
        bindPreferences();
    }

    private void readKeys() {
        SETTINGS_ENABLED_KEY = getString(R.string.fragment_adblock_settings_enabled_key);
        SETTINGS_FILTER_LISTS_KEY = getString(R.string.fragment_adblock_settings_filter_lists_key);
        SETTINGS_FILTER_LISTS_ADD_KEY = getString(R.string.fragment_adblock_settings_filter_lists_add_key);
        SETTINGS_FILTER_LISTS_CLEAR_KEY = getString(R.string.fragment_adblock_settings_filter_lists_clear_key);
        SETTINGS_FILTER_MY_KEY = getString(R.string.fragment_adblock_settings_filter_my_key);
        SETTINGS_AA_ENABLED_KEY = getString(R.string.fragment_adblock_settings_aa_enabled_key);
        SETTINGS_WL_DOMAINS_KEY = getString(R.string.fragment_adblock_settings_wl_key);
        SETTINGS_ALLOWED_CONNECTION_TYPE_KEY = getString(R.string.fragment_adblock_settings_allowed_connection_type_key);
    }

    private void bindPreferences() {
        adblockEnabled = (SwitchPreferenceCompat) findPreference(SETTINGS_ENABLED_KEY);
        filterLists = (MultiSelectListPreference) findPreference(SETTINGS_FILTER_LISTS_KEY);
        filterListsAdd = findPreference(SETTINGS_FILTER_LISTS_ADD_KEY);
        acceptableAdsEnabled = (SwitchPreferenceCompat) findPreference(SETTINGS_AA_ENABLED_KEY);
        whitelistedDomains = findPreference(SETTINGS_WL_DOMAINS_KEY);
        filterMy = findPreference(SETTINGS_FILTER_MY_KEY);
        filterClear = findPreference(SETTINGS_FILTER_LISTS_CLEAR_KEY);
        allowedConnectionType = (ListPreference) findPreference(SETTINGS_ALLOWED_CONNECTION_TYPE_KEY);
        initAllowNetwork();
        filterLists.setEntries(new String[]{});
        filterLists.setEntryValues(new String[]{});
        Preference preference = findPreference(getString(R.string.fragment_adblock_settings_filter_count_key));
        try {
            preference.setTitle("已拦截广告数：" + PreferenceMgr.getLong(getContext(), "adblockplus_count", 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPreferences() {
        initEnabled();
        initFilterLists();
        initAcceptableAdsEnabled();
        initWhitelistedDomains();
        initUpdatesConnection();
    }

    private void initAllowNetwork() {
        CharSequence[] values =
                {
                        ConnectionType.WIFI_NON_METERED.getValue(),
                        ConnectionType.WIFI.getValue(),
                        ConnectionType.ANY.getValue()
                };

        CharSequence[] titles =
                {
                        getString(R.string.fragment_adblock_settings_allowed_connection_type_wifi_non_metered),
                        getString(R.string.fragment_adblock_settings_allowed_connection_type_wifi),
                        getString(R.string.fragment_adblock_settings_allowed_connection_type_all),
                };

        allowedConnectionType.setEntryValues(values);
        allowedConnectionType.setEntries(titles);
    }

    private void initUpdatesConnection() {

        // selected value
        ConnectionType connectionType = settings.getAllowedConnectionType();
        if (connectionType == null) {
            connectionType = ConnectionType.ANY;
        }
        allowedConnectionType.setValue(connectionType.getValue());
        allowedConnectionType.setOnPreferenceChangeListener(this);
    }

    private void initWhitelistedDomains() {
        whitelistedDomains.setOnPreferenceClickListener(this);
        filterListsAdd.setOnPreferenceClickListener(this);
        filterMy.setOnPreferenceClickListener(this);
        filterClear.setOnPreferenceClickListener(this);
    }

    private void initAcceptableAdsEnabled() {
        acceptableAdsEnabled.setChecked(settings.isAcceptableAdsEnabled());
        acceptableAdsEnabled.setOnPreferenceChangeListener(this);
    }

    private MySubscription[] pureSubs(MySubscription[] subscriptions) {
        List<MySubscription> mySubscriptions = new ArrayList<>();
        for (MySubscription subscription : subscriptions) {
            if (!subscription.url.contains("+easylist")) {
                mySubscriptions.add(subscription);
            } else if (subscription.url.contains("abpvn+")
                    || subscription.url.contains("abpindo+")
                    || subscription.url.contains("china")
                    || subscription.url.contains("liste_")
                    || subscription.url.contains("rolist+")
                    || subscription.url.contains("ruadlist+")
                    || subscription.url.contains("bulgarian+")) {
                mySubscriptions.add(subscription);
            }
        }
        MySubscription[] result = new MySubscription[mySubscriptions.size()];
        return mySubscriptions.toArray(result);
    }

    public org.adblockplus.libadblockplus.android.settings.MySubscription[] getRecommendedSubscriptions() {
        List<org.adblockplus.libadblockplus.Subscription> subscriptions = provider.getAdblockEngine().getFilterEngine().fetchAvailableSubscriptions();
        List<org.adblockplus.libadblockplus.Subscription> subed = provider.getAdblockEngine().getFilterEngine().getListedSubscriptions();
        try {
            MySubscription[] subscriptions1 = Utils.convertJsSubscriptions(subscriptions);
            subscriptions1 = pureSubs(subscriptions1);
            recommendSubs.clear();
            recommendSubs.addAll(Arrays.asList(subscriptions1));
            Set<String> subs = new HashSet<>();
            List<MySubscription> subscriptionList = new ArrayList<>(Arrays.asList(subscriptions1));
            for (MySubscription eachSubscription : subscriptions1) {
                subs.add(eachSubscription.url);
            }
            MySubscription[] subed2 = Utils.convertJsSubscriptions(subed);
            mySubs.clear();
            mySubs.addAll(Arrays.asList(subed2));
            //推荐的加上订阅的
            for (MySubscription eachSubscription : subed2) {
                if (!subs.contains(eachSubscription.url)) {
                    subs.add(eachSubscription.url);
                    subscriptionList.add(0, eachSubscription);
                }
            }
            //加上取消订阅的
            try {
                String notSubAbpUrls = PreferenceMgr.getString(getContext(), NOT_SUB_ABP_URLS, "");
                String[] urls = notSubAbpUrls.split("&&");
                if (urls.length > 0) {
                    for (String url : urls) {
                        String[] titleUrl = url.split("@");
                        if (titleUrl.length == 2) {
                            if (!subs.contains(titleUrl[1])) {
                                MySubscription subscription = new MySubscription();
                                subscription.title = titleUrl[0];
                                subscription.url = titleUrl[1];
                                subs.add(titleUrl[1]);
                                subscriptionList.add(subscription);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            MySubscription[] result = new MySubscription[subscriptionList.size()];
            return subscriptionList.toArray(result);
        } finally {
            for (org.adblockplus.libadblockplus.Subscription eachSubscription : subscriptions) {
                eachSubscription.dispose();
            }
        }
    }

    private void initFilterLists() {
//        final Map<String, String> localeToTitle = Utils.getLocaleToTitleMap(getContext());

        // all available values
        CharSequence[] availableSubscriptionsTitles = new CharSequence[availableSubscriptions.length];
        CharSequence[] availableSubscriptionsValues = new CharSequence[availableSubscriptions.length];
        for (int i = 0; i < availableSubscriptions.length; i++) {
            if (availableSubscriptions[i].errors > 0) {
                availableSubscriptionsTitles[i] = availableSubscriptions[i].title + "（出错、请检查规则是否正确或者网络是否流畅）";
            } else {
                availableSubscriptionsTitles[i] = availableSubscriptions[i].title;
            }
            availableSubscriptionsValues[i] = availableSubscriptions[i].url;
        }
        filterLists.setEntries(availableSubscriptionsTitles);
        filterLists.setEntryValues(availableSubscriptionsValues);

        // selected values
        Set<String> selectedSubscriptionValues = new HashSet<>();
        for (Subscription eachSubscription : settings.getSubscriptions()) {
            selectedSubscriptionValues.add(eachSubscription.url);
        }
        for (MySubscription mySub : mySubs) {
            selectedSubscriptionValues.add(mySub.url);
        }
        filterMy.setTitle("我的订阅 已订阅" + selectedSubscriptionValues.size() + "个地址");
        filterLists.setValues(selectedSubscriptionValues);
        selectedFilterUrls.clear();
        selectedFilterUrls.addAll(selectedSubscriptionValues);
        filterLists.setOnPreferenceChangeListener(this);
    }

    private void initEnabled() {
        boolean enabled = settings.isAdblockEnabled();
        adblockEnabled.setChecked(enabled);
        adblockEnabled.setOnPreferenceChangeListener(this);
        applyAdblockEnabled(enabled);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Timber.d("\"%s\" new value is %s", preference.getTitle(), newValue);

        if (preference.getKey().equals(SETTINGS_ENABLED_KEY)) {
            handleEnabledChanged((Boolean) newValue);
        } else if (preference.getKey().equals(SETTINGS_FILTER_LISTS_KEY)) {
            //noinspection unchecked
            handleFilterListsChanged((Set<String>) newValue);
        } else if (preference.getKey().equals(SETTINGS_AA_ENABLED_KEY)) {
            handleAcceptableAdsEnabledChanged((Boolean) newValue);
        } else if (preference.getKey().equals(SETTINGS_ALLOWED_CONNECTION_TYPE_KEY)) {
            handleAllowedConnectionTypeChanged((String) newValue);
        } else {
            // handle other values if changed
            // `false` for NOT update preference view state
            return false;
        }

        // `true` for update preference view state
        return true;
    }

    private void handleAllowedConnectionTypeChanged(String value) {
        // update and save settings
        settings.setAllowedConnectionType(ConnectionType.findByValue(value));
        provider.getAdblockSettingsStorage().save(settings);

        // apply settings
        allowedConnectionType.setValue(value);
        provider.getAdblockEngine().getFilterEngine().setAllowedConnectionType(value);

        // signal event
        listener.onAdblockSettingsChanged(this);

    }

    private void handleAcceptableAdsEnabledChanged(Boolean newValue) {
        boolean enabledValue = newValue;

        // update and save settings
        settings.setAcceptableAdsEnabled(enabledValue);
        provider.getAdblockSettingsStorage().save(settings);

        // apply settings
        provider.getAdblockEngine().setAcceptableAdsEnabled(enabledValue);

        // signal event
        listener.onAdblockSettingsChanged(this);
    }

    private void handleFilterListsChanged(Set<String> newValue) {

        List<Subscription> selectedSubscriptions = new LinkedList<>();
        List<String> unSelectedTitles = new ArrayList<>();
        List<String> unSelectedUrls = new ArrayList<>();
        Set<String> recommendUrls = new HashSet<>();
        if (recommendSubs != null && !recommendSubs.isEmpty()) {
            for (MySubscription recommendSub : recommendSubs) {
                recommendUrls.add(recommendSub.url);
            }
        }

        for (MySubscription eachSubscription : availableSubscriptions) {
            if (newValue.contains(eachSubscription.url)) {
                selectedSubscriptions.add(Utils.convertSubscription(eachSubscription));
            } else if (!recommendUrls.contains(eachSubscription.url)) {
                unSelectedTitles.add(eachSubscription.title);
                unSelectedUrls.add(eachSubscription.url);
            }
        }

        //存储取消订阅的地址
        if (!unSelectedTitles.isEmpty()) {
            StringBuilder builder = new StringBuilder(unSelectedTitles.get(0) + "@" + unSelectedUrls.get(0));
            for (int i = 1; i < unSelectedTitles.size(); i++) {
                builder.append("&&").append(unSelectedTitles.get(i)).append("@").append(unSelectedUrls.get(i));
            }
            PreferenceMgr.put(getContext(), NOT_SUB_ABP_URLS, builder.toString());
        } else {
            PreferenceMgr.put(getContext(), NOT_SUB_ABP_URLS, "");
        }

        // update and save settings
        settings.setSubscriptions(selectedSubscriptions);
        provider.getAdblockSettingsStorage().save(settings);

        // apply settings
//        provider.getAdblockEngine().clearSubscriptions();
        setSubscriptions(newValue);

        // since 'aa enabled' setting affects subscriptions list, we need to set it again
        provider.getAdblockEngine().setAcceptableAdsEnabled(settings.isAcceptableAdsEnabled());

        // signal event
        listener.onAdblockSettingsChanged(this);

        refreshFilters();

    }

    private void handleEnabledChanged(boolean newValue) {
        // update and save settings
        settings.setAdblockEnabled(newValue);
        provider.getAdblockSettingsStorage().save(settings);

        // apply settings
        provider.getAdblockEngine().setEnabled(newValue);

        // signal event
        listener.onAdblockSettingsChanged(this);

        // all other settings are meaningless if ad blocking is disabled
        applyAdblockEnabled(newValue);
    }

    private void applyAdblockEnabled(boolean enabledValue) {
        filterLists.setEnabled(enabledValue);
        acceptableAdsEnabled.setEnabled(enabledValue);
        whitelistedDomains.setEnabled(enabledValue);
        allowedConnectionType.setEnabled(enabledValue);
    }

    private void addSub() {
        new XPopup.Builder(getContext()).asInputConfirm("添加订阅地址", "请输入拦截规则的订阅地址",
                text -> {
                    if (TextUtils.isEmpty(text)) {
                        ToastMgr.shortBottomCenter(getContext(), "地址不能为空");
                        return;
                    }
                    if (!text.toLowerCase().startsWith("http")) {
                        ToastMgr.shortBottomCenter(getContext(), "地址不合法，必须http开头");
                        return;
                    }
                    for (MySubscription availableSubscription : availableSubscriptions) {
                        if (availableSubscription.url.equals(text)) {
                            ToastMgr.shortBottomCenter(getContext(), "订阅列表已包含该地址，订阅名称为" + availableSubscription.title);
                            return;
                        }
                    }
                    Set<String> urls = filterLists.getValues();
                    if (urls == null) {
                        urls = new HashSet<>();
                    }
                    urls.add(text);
//                    filterLists.setValues(urls);
                    MySubscription subscription = new MySubscription();
                    subscription.title = text;
                    subscription.url = text;
                    MySubscription[] newSubs = new MySubscription[availableSubscriptions.length + 1];
                    System.arraycopy(availableSubscriptions, 0, newSubs, 0, availableSubscriptions.length);
                    newSubs[newSubs.length - 1] = subscription;
                    availableSubscriptions = newSubs;
                    handleFilterListsChanged(urls);
                })
                .show();
    }

    private void refreshFilters() {
        if (loadingPopupView == null) {
            loadingPopupView = new XPopup.Builder(getContext()).asLoading();
        }
        loadingPopupView.setTitle("正在刷新订阅地址，请稍候");
        loadingPopupView.show();
        new Thread(() -> {
            availableSubscriptions = getRecommendedSubscriptions();
            // selected values
            Set<String> selectedSubscriptionValues = new HashSet<>();
            for (Subscription eachSubscription : settings.getSubscriptions()) {
                selectedSubscriptionValues.add(eachSubscription.url);
            }
            for (MySubscription mySub : mySubs) {
                if (!selectedSubscriptionValues.contains(mySub.url)) {
                    selectedSubscriptionValues.add(mySub.url);
                }
            }

            for (MySubscription availableSubscription : availableSubscriptions) {
                if (selectedSubscriptionValues.contains(availableSubscription.url) &&
                        availableSubscription.size <= 0) {
                    try {
                        org.adblockplus.libadblockplus.Subscription subscription = AdblockHelper.get().getProvider().getEngine()
                                .getFilterEngine()
                                .getSubscription(availableSubscription.url);
                        int checkCount = 0;
                        while (subscription.isUpdating() && checkCount < 30) {
                            checkCount++;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        JsValue jsText = subscription.getProperty("_filterText");
                        JsValue jsTitle = subscription.getProperty("title");
                        JsValue jsLastSuccess = subscription.getProperty("lastSuccess");

                        try {
                            availableSubscription.lastSuccess = jsLastSuccess.asLong();
                        } finally {
                            jsLastSuccess.dispose();
                        }
                        try {
                            availableSubscription.title = jsTitle.toString();
                        } finally {
                            jsTitle.dispose();
                        }
                        try {
                            String text = jsText.toString();
                            if (!"".equals(text)) {
                                String[] rules = text.split(",");
                                int count = 0;
                                for (String rule : rules) {
                                    if (!rule.startsWith("!")) {
                                        count++;
                                    }
                                }
                                availableSubscription.size = count;
                            }
                        } finally {
                            jsText.dispose();
                            subscription.dispose();
                        }
                    } catch (Throwable e) {
                        Log.e(TAG, "refreshFilters: " + e.getMessage(), e);
                    }
                }
            }

            if (getActivity() != null) {
                if (getActivity().isFinishing()) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    if (getActivity().isFinishing()) {
                        return;
                    }
                    CharSequence[] availableSubscriptionsTitles = new CharSequence[availableSubscriptions.length];
                    CharSequence[] availableSubscriptionsValues = new CharSequence[availableSubscriptions.length];
                    for (int i = 0; i < availableSubscriptions.length; i++) {
                        if (availableSubscriptions[i].errors > 0) {
                            availableSubscriptionsTitles[i] = availableSubscriptions[i].title + " （出错、请检查规则是否正确或者网络是否流畅）";
                        } else {
                            availableSubscriptionsTitles[i] = availableSubscriptions[i].title;
                        }
                        availableSubscriptionsValues[i] = availableSubscriptions[i].url;
                    }
                    filterLists.setEntries(availableSubscriptionsTitles);
                    filterLists.setEntryValues(availableSubscriptionsValues);
                    filterLists.setValues(selectedSubscriptionValues);
                    selectedFilterUrls.clear();
                    selectedFilterUrls.addAll(selectedSubscriptionValues);
                    filterMy.setTitle("我的订阅 已订阅" + selectedSubscriptionValues.size() + "个地址");
                    loadingPopupView.dismiss();
                });
            }
        }).start();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(SETTINGS_WL_DOMAINS_KEY)) {
            listener.onWhitelistedDomainsClicked(this);
        } else if (preference.getKey().equals(SETTINGS_FILTER_LISTS_CLEAR_KEY)) {
            clearNotSubedUrls();
        } else if (preference.getKey().equals(SETTINGS_FILTER_LISTS_ADD_KEY)) {
            addSub();
        } else if (preference.getKey().equals(SETTINGS_FILTER_MY_KEY)) {
            showMySub();
        } else {
            // should not be invoked as only 'wl' preference is subscribed for callback
            return false;
        }

        return true;
    }

    private void clearNotSubedUrls() {
        new XPopup.Builder(getContext())
                .asConfirm("温馨提示", "是否清除已取消订阅的地址，如果不清除取消订阅的地址依然可以再次订阅，清除后则不会再显示在订阅列表中", new OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        PreferenceMgr.put(getContext(), NOT_SUB_ABP_URLS, "");
                        refreshFilters();
                    }
                }).show();
    }

    private void showMySub() {
        if (mySubs == null || mySubs.isEmpty()) {
            return;
        }
        new XPopup.Builder(getContext())
                .moveUpToKeyboard(false) //如果不加这个，评论弹窗会移动到软键盘上面
                .asCustom(new MySubsPopup(getContext()).with(mySubs).withTitle("我的订阅"))
                .show();
    }


    public void setSubscriptions(Collection<String> urls) {
        FilterEngine filterEngine = provider.getAdblockEngine().getFilterEngine();
        setSubscriptions(urls, filterEngine);
    }

    public static void updateSubscriptions(List<Subscription> subscriptions) {
        FilterEngine filterEngine = AdblockHelper.get().getProvider().getEngine().getFilterEngine();
        Set<String> urls = new HashSet<>();
        for (Subscription subscription : subscriptions) {
            urls.add(subscription.url);
        }
        setSubscriptions(urls, filterEngine);
    }


    public static void setSubscriptions(final Collection<String> urls, FilterEngine filterEngine) {
        final List<org.adblockplus.libadblockplus.Subscription> currentSubscriptions = filterEngine.getListedSubscriptions();
        // remove the removed ones
        for (final org.adblockplus.libadblockplus.Subscription eachCurrentSubscription : currentSubscriptions) {
            try {
                final JsValue jsUrl = eachCurrentSubscription.getProperty("url");
                if (jsUrl != null) {
                    String eachCurrentUrl;
                    try {
                        eachCurrentUrl = jsUrl.asString();
                    } finally {
                        jsUrl.dispose();
                    }

                    if (!urls.contains(eachCurrentUrl)) {
                        eachCurrentSubscription.removeFromList();
                    }
                }
            } finally {
                eachCurrentSubscription.dispose();
            }
        }

        // add new subscriptions
        for (final String eachNewUrl : urls) {
            final org.adblockplus.libadblockplus.Subscription eachNewSubscription = filterEngine.getSubscription(eachNewUrl);

            if (eachNewSubscription != null) {
                try {
                    Field field = AdblockHelper.get().getProvider().getEngine().getClass().getDeclaredField("platform");
                    field.setAccessible(true);
                    Platform platform = (Platform) field.get(AdblockHelper.get().getProvider().getEngine());
                    eachNewSubscription.setProperty("_lastDownload", platform.getJsEngine().newValue(0L));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if (!eachNewSubscription.isListed()) {
                        eachNewSubscription.addToList();
                    }
                } finally {
                    eachNewSubscription.dispose();
                }
            }
        }
    }
}
