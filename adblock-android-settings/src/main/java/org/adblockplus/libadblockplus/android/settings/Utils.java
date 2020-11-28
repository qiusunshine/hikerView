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
import android.content.res.Resources;
import android.text.TextUtils;

import org.adblockplus.libadblockplus.JsValue;
import org.adblockplus.libadblockplus.Subscription;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {
    private static final String TAG = "Utils";

    public static String getDom(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        try {
            url = url.replaceFirst("http://", "").replaceFirst("https://", "");
            String[] urls = url.split("/");
            if (urls.length > 0) {
                return urls[0];
            }
        } catch (Exception e) {
            return null;
        }
        return url;
    }

    public static Map<String, String> getLocaleToTitleMap(final Context context) {
        final Resources resources = context.getResources();
        final String[] locales = resources.getStringArray(R.array.fragment_adblock_general_locale_title);
        final String separator = resources.getString(R.string.fragment_adblock_general_separator);
        final Map<String, String> localeToTitle = new HashMap<>(locales.length);
        for (final String localeAndTitlePair : locales) {
            // in `String.split()` separator is a regexp, but we want to treat it as a string
            final int separatorIndex = localeAndTitlePair.indexOf(separator);
            final String locale = localeAndTitlePair.substring(0, separatorIndex);
            final String title = localeAndTitlePair.substring(separatorIndex + 1);
            localeToTitle.put(locale, title);
        }
        return localeToTitle;
    }

    public static org.adblockplus.libadblockplus.android.settings.MySubscription[] convertJsSubscriptions(
            final List<Subscription> jsSubscriptions) {
        if (jsSubscriptions == null || jsSubscriptions.isEmpty()) {
            return new MySubscription[]{};
        }
        final org.adblockplus.libadblockplus.android.settings.MySubscription[] subscriptions =
                new org.adblockplus.libadblockplus.android.settings.MySubscription[jsSubscriptions.size()];

        for (int i = 0; i < subscriptions.length; i++) {
            subscriptions[i] = convertJsSubscription(jsSubscriptions.get(i));
        }

        return subscriptions;
    }
    public static org.adblockplus.libadblockplus.android.Subscription convertSubscription(MySubscription mySubscription) {
        org.adblockplus.libadblockplus.android.Subscription subscription = new org.adblockplus.libadblockplus.android.Subscription();
        subscription.title = mySubscription.title;
        subscription.url = mySubscription.url;
        subscription.author = mySubscription.author;
        subscription.prefixes = mySubscription.prefixes;
        subscription.homepage = mySubscription.homepage;
        return subscription;
    }

    private static org.adblockplus.libadblockplus.android.settings.MySubscription convertJsSubscription(final Subscription jsSubscription) {
        final org.adblockplus.libadblockplus.android.settings.MySubscription subscription =
                new org.adblockplus.libadblockplus.android.settings.MySubscription();

        JsValue jsTitle = jsSubscription.getProperty("title");
        try {
            subscription.title = jsTitle.toString();
        } finally {
            jsTitle.dispose();
        }

        JsValue jsError = jsSubscription.getProperty("_errors");
        try {
            subscription.errors = jsError.asLong();
        } finally {
            jsError.dispose();
        }

        JsValue jsLastSuccess = jsSubscription.getProperty("lastSuccess");
        try {
            subscription.lastSuccess = jsLastSuccess.asLong();
//            Log.d(TAG, "convertJsSubscription: " + subscription.lastSuccess);
        } finally {
            jsLastSuccess.dispose();
        }

        JsValue jsUrl = jsSubscription.getProperty("url");
        try {
            subscription.url = jsUrl.toString();
        } finally {
            jsUrl.dispose();
        }

        JsValue jsText = jsSubscription.getProperty("_filterText");
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
                subscription.size = count;
            }
        } finally {
            jsText.dispose();
        }

        JsValue jsPrefixes = jsSubscription.getProperty("prefixes");
        try {
            if (!jsPrefixes.isUndefined() && !jsPrefixes.isNull())
                subscription.prefixes = jsPrefixes.asString();
        } finally {
            jsPrefixes.dispose();
        }

        return subscription;
    }



    public static String getAutoTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(Calendar.HOUR_OF_DAY, 0);
        currentCalendar.set(Calendar.MINUTE, 0);
        currentCalendar.set(Calendar.SECOND, 0);
        currentCalendar.set(Calendar.MILLISECOND, 0);
        long dateL = calendar.getTimeInMillis();
        long nowL = currentCalendar.getTimeInMillis();
        if (dateL < nowL - 1296000000L) { // 比15天前还早
            if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) { // 在今年
                return getDate(date);
            } else {
                return getDateOneYear(date);
            }
        } else if (dateL < nowL - 172800000L) { // 比今天0时还早48小时以上
            return ((nowL - dateL) / 86400000L) + "天前";
        } else if (dateL < nowL - 86400000L) { // 比今天0时还早24小时以上
            return "前天" + getTime(date);
        } else if (dateL < nowL) { // 比今天0时还早
            return "昨天" + getTime(date);
        } else if (dateL < nowL + 86400000L) { // 明天0时前
            return "今天" + getTime(date);
        } else if (dateL < nowL + 172800000L) { // 晚今天0时48小时内
            return "明天" + getTime(date);
        } else if (dateL < nowL + 259200000L) { // 晚今天0时72小时内
            return "后天" + getTime(date);
        } else {
            return getDateOneYear(date);
        }
    }

    public static String getBaseDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:00", Locale.getDefault());
        return sdf.format(date);
    }
    public static String getDateOneYear(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        return sdf.format(date);
    }

    public static String getDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日", Locale.getDefault());
        return sdf.format(date);
    }

    public static String getTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    public static String getWeek(int value) {
        switch (value) {
            case 1:
                return "周日";
            case 2:
                return "周一";
            case 3:
                return "周二";
            case 4:
                return "周三";
            case 5:
                return "周四";
            case 6:
                return "周五";
            default:
                return "周六";
        }
    }
}
