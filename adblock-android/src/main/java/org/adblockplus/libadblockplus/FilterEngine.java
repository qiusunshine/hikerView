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

package org.adblockplus.libadblockplus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FilterEngine
{
  protected final long ptr;

  static
  {
    System.loadLibrary(BuildConfig.nativeLibraryName);
    registerNatives();
  }

  public enum ContentType
  {
    OTHER, SCRIPT, IMAGE, STYLESHEET, OBJECT, SUBDOCUMENT, DOCUMENT, WEBSOCKET,
    WEBRTC, PING, XMLHTTPREQUEST, OBJECT_SUBREQUEST, MEDIA, FONT, GENERICBLOCK,
    ELEMHIDE, GENERICHIDE;

    public static Set<ContentType> maskOf(ContentType... contentTypes)
    {
      final Set<ContentType> set = new HashSet<>(contentTypes.length);
      for (ContentType contentType : contentTypes)
      {
        set.add(contentType);
      }
      return set;
    }
  }

  public static class EmulationSelector
  {
    public String selector;
    public String text;

    public EmulationSelector(String selector, String text)
    {
      this.selector = selector;
      this.text = text;
    }
  }

  FilterEngine(long jniPlatformPtr)
  {
    this.ptr = jniPlatformPtr;
  }

  public boolean isFirstRun()
  {
    return isFirstRun(this.ptr);
  }

  public Filter getFilter(final String text)
  {
    return getFilter(this.ptr, text);
  }

  public List<Filter> getListedFilters()
  {
    return getListedFilters(this.ptr);
  }

  public Subscription getSubscription(final String url)
  {
    return getSubscription(this.ptr, url);
  }

  public List<Subscription> getListedSubscriptions()
  {
    return getListedSubscriptions(this.ptr);
  }

  public List<Subscription> fetchAvailableSubscriptions()
  {
    return fetchAvailableSubscriptions(this.ptr);
  }

  public void removeFilterChangeCallback()
  {
    removeFilterChangeCallback(this.ptr);
  }

  public void setFilterChangeCallback(final FilterChangeCallback callback)
  {
    setFilterChangeCallback(this.ptr, callback.ptr);
  }

  public String getElementHidingStyleSheet(final String domain)
  {
    return getElementHidingStyleSheet(domain, false);
  }

  public String getElementHidingStyleSheet(final String domain, final boolean specificOnly)
  {
    return getElementHidingStyleSheet(this.ptr, domain, specificOnly);
  }

  public List<EmulationSelector> getElementHidingEmulationSelectors(final String domain)
  {
    return getElementHidingEmulationSelectors(this.ptr, domain);
  }

  public void showNextNotification()
  {
    showNextNotification(this.ptr);
  }

  public void setShowNotificationCallback(final ShowNotificationCallback callback)
  {
    setShowNotificationCallback(this.ptr, callback.ptr);
  }

  public void removeShowNotificationCallback()
  {
    removeShowNotificationCallback(this.ptr);
  }

  /**
   * Checks if any active filter matches the supplied URL.
   * @param url url URL to match.
   * @param contentTypes Content type mask of the requested resource.
   * @param documentUrls Chain of documents requesting the resource, starting
   *                     with the current resource's parent frame, ending with the
   *                     top-level frame.
   * @param siteKey sitekey or null/empty string
   * @return Matching filter, or a `null` if there was no match.
   */
  public Filter matches(final String url, final Set<ContentType> contentTypes,
                        final List<String> documentUrls, final String siteKey)
  {
    return matches(this.ptr, url, contentTypes.toArray(new ContentType[contentTypes.size()]),
            documentUrls, siteKey, false);
  }

  /**
   * Checks if any active filter matches the supplied URL.
   * @param url url URL to match.
   * @param contentTypes Content type mask of the requested resource.
   * @param documentUrls Chain of documents requesting the resource, starting
   *                     with the current resource's parent frame, ending with the
   *                     top-level frame.
   * @param siteKey sitekey or null/empty string
   * @param specificOnly if set to `true` then skips generic filters
   * @return Matching filter, or a `null` if there was no match.
   */
  public Filter matches(final String url, final Set<ContentType> contentTypes,
                        final List<String> documentUrls, final String siteKey,
                        final boolean specificOnly)
  {
    return matches(this.ptr, url, contentTypes.toArray(new ContentType[contentTypes.size()]),
            documentUrls, siteKey, specificOnly);
  }

  /**
   * Checks if any active filter matches the supplied URL.
   * @param url URL to match which is actually first parent of URL for which we
   *            want to check a $genericblock filter.
   *            Value obtained by `IsGenericblockWhitelisted()` is used later
   *            on as a `specificOnly` parameter value for `Matches()` call.
   * @param documentUrls Chain of documents requesting the resource, starting
   *                     with the current resource's parent frame, ending with the
   *                     top-level frame.
   * @param siteKey sitekey or null/empty string
   * @return `true` if the URL is whitelisted by $genericblock filter
   */
  public boolean isGenericblockWhitelisted(final String url, final List<String> documentUrls,
                                           final String siteKey)
  {
    return isGenericblockWhitelisted(this.ptr, url, documentUrls, siteKey);
  }

  /**
   * Check if the document with URL is whitelisted
   * @param url URL
   * @param documentUrls Chain of document URLs requesting the document,
   *                     starting with the current document's parent frame, ending with
   *                     the top-level frame.
   * @param siteKey sitekey or null/empty string
   * @return `true` if the URL is whitelisted
   */
  public boolean isDocumentWhitelisted(final String url,
                                       final List<String> documentUrls,
                                       final String siteKey)
  {
    return isDocumentWhitelisted(this.ptr, url, documentUrls, siteKey);
  }

  /**
   * Check if the element hiding is whitelisted
   * @param url URL
   * @param documentUrls Chain of document URLs requesting the document,
   *                     starting with the current document's parent frame, ending with
   *                     the top-level frame.
   * @param siteKey sitekey or null/empty string
   * @return `true` if element hiding is whitelisted for the supplied URL.
   */
  public boolean isElemhideWhitelisted(final String url,
                                       final List<String> documentUrls,
                                       final String siteKey)
  {
    return isElemhideWhitelisted(this.ptr, url, documentUrls, siteKey);
  }

  public JsValue getPref(final String pref)
  {
    return getPref(this.ptr, pref);
  }

  /**
   * Set libadblockplus preference. Only known preferences will be stored after engine is disposed.
   * @param pref preference name. See lib/pref.js in libadblockplus for valid names
   * @param value preference value
   */
  public void setPref(final String pref, final JsValue value)
  {
    setPref(this.ptr, pref, value.ptr);
  }

  public String getHostFromURL(String url)
  {
    return getHostFromURL(this.ptr, url);
  }

  public void setAllowedConnectionType(String value)
  {
    setAllowedConnectionType(this.ptr, value);
  }

  public String getAllowedConnectionType()
  {
    return getAllowedConnectionType(this.ptr);
  }

  public void setAcceptableAdsEnabled(final boolean enabled)
  {
    setAcceptableAdsEnabled(this.ptr, enabled);
  }

  public boolean isAcceptableAdsEnabled()
  {
    return isAcceptableAdsEnabled(this.ptr);
  }

  public String getAcceptableAdsSubscriptionURL()
  {
    return getAcceptableAdsSubscriptionURL(this.ptr);
  }

  /**
   * Schedules updating of a subscription corresponding to the passed URL.
   * @param subscriptionUrl may contain query parameters, only the beginning of the string is used
   *                       to find a corresponding subscription.
   */
  public void updateFiltersAsync(String subscriptionUrl)
  {
    updateFiltersAsync(this.ptr, subscriptionUrl);
  }

  /**
   * Get FilterEngine pointer
   * @return C++ FilterEngine instance pointer (AdblockPlus::FilterEngine*)
   */
  public long getNativePtr()
  {
    return getNativePtr(this.ptr);
  }

  private final static native void registerNatives();

  private final static native boolean isFirstRun(long ptr);

  private final static native Filter getFilter(long ptr, String text);

  private final static native List<Filter> getListedFilters(long ptr);

  private final static native Subscription getSubscription(long ptr, String url);

  private final static native List<Subscription> getListedSubscriptions(long ptr);

  private final static native List<Subscription> fetchAvailableSubscriptions(long ptr);

  private final static native void removeFilterChangeCallback(long ptr);

  private final static native void setFilterChangeCallback(long ptr, long filterPtr);

  private final static native String getElementHidingStyleSheet(long ptr, String domain, boolean specificOnly);

  private final static native List<EmulationSelector> getElementHidingEmulationSelectors(long ptr, String domain);

  private final static native void showNextNotification(long ptr);

  private final static native void setShowNotificationCallback(long ptr, long callbackPtr);

  private final static native void removeShowNotificationCallback(long ptr);

  private final static native JsValue getPref(long ptr, String pref);

  private final static native Filter matches(long ptr, String url, ContentType[] contentType,
                                             List<String> referrerChain, String siteKey,
                                             boolean specificOnly);

  private final static native boolean isGenericblockWhitelisted(long ptr, String url,
                                                                List<String> referrerChain,
                                                                String siteKey);

  private final static native boolean isDocumentWhitelisted(long ptr, String url,
                                                            List<String> referrerChain,
                                                            String siteKey);

  private final static native boolean isElemhideWhitelisted(long ptr, String url,
                                                            List<String> referrerChain,
                                                            String siteKey);

  private final static native void setPref(long ptr, String pref, long valuePtr);

  private final static native String getHostFromURL(long ptr, String url);

  private final static native void setAllowedConnectionType(long ptr, String value);

  private final static native String getAllowedConnectionType(long ptr);

  private final static native void setAcceptableAdsEnabled(long ptr, boolean enabled);

  private final static native boolean isAcceptableAdsEnabled(long ptr);

  private final static native String getAcceptableAdsSubscriptionURL(long ptr);

  private final static native void updateFiltersAsync(long ptr, String subscriptionUrl);

  private final static native long getNativePtr(long ptr);
}
