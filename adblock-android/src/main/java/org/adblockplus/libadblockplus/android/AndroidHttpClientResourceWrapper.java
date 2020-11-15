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

import android.content.Context;
import android.content.SharedPreferences;
import timber.log.Timber;

import org.adblockplus.libadblockplus.HttpClient;
import org.adblockplus.libadblockplus.HttpRequest;
import org.adblockplus.libadblockplus.ServerResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HttpClient wrapper to return request response from android resources for selected URLs
 */
public class AndroidHttpClientResourceWrapper extends HttpClient
{
  public static final String EASYLIST =
    "https://easylist-downloads.adblockplus.org/easylist.txt";
  public static final String EASYLIST_INDONESIAN =
    "https://easylist-downloads.adblockplus.org/abpindo+easylist.txt";
  public static final String EASYLIST_BULGARIAN =
    "https://easylist-downloads.adblockplus.org/bulgarian_list+easylist.txt";
  public static final String EASYLIST_CHINESE =
    "https://easylist-downloads.adblockplus.org/easylistchina+easylist.txt";
  public static final String EASYLIST_CZECH_SLOVAK =
    "https://easylist-downloads.adblockplus.org/easylistczechslovak+easylist.txt";
  public static final String EASYLIST_DUTCH =
    "https://easylist-downloads.adblockplus.org/easylistdutch+easylist.txt";
  public static final String EASYLIST_GERMAN =
    "https://easylist-downloads.adblockplus.org/easylistgermany+easylist.txt";
  public static final String EASYLIST_ISRAELI =
    "https://easylist-downloads.adblockplus.org/israellist+easylist.txt";
  public static final String EASYLIST_ITALIAN =
    "https://easylist-downloads.adblockplus.org/easylistitaly+easylist.txt";
  public static final String EASYLIST_LITHUANIAN =
    "https://easylist-downloads.adblockplus.org/easylistlithuania+easylist.txt";
  public static final String EASYLIST_LATVIAN =
    "https://easylist-downloads.adblockplus.org/latvianlist+easylist.txt";
  public static final String EASYLIST_ARABIAN_FRENCH =
    "https://easylist-downloads.adblockplus.org/liste_ar+liste_fr+easylist.txt";
  public static final String EASYLIST_FRENCH =
    "https://easylist-downloads.adblockplus.org/liste_fr+easylist.txt";
  public static final String EASYLIST_ROMANIAN =
    "https://easylist-downloads.adblockplus.org/rolist+easylist.txt";
  public static final String EASYLIST_RUSSIAN =
    "https://easylist-downloads.adblockplus.org/ruadlist+easylist.txt";
  public static final String ACCEPTABLE_ADS =
    "https://easylist-downloads.adblockplus.org/exceptionrules.txt";

  private Context context;
  private HttpClient httpClient;
  private Map<String, Integer> urlToResourceIdMap;
  private Storage storage;
  private Listener listener;

  /**
   * Constructor
   * @param context android context
   * @param httpClient wrapped http client to perform the request if it's not preloaded subscription requested
   * @param urlToResourceIdMap map URL to android resource id for preloaded subscriptions
   *                           See AndroidHttpClientResourceWrapper.EASYLIST_... constants
   * @param storage Storage impl to remember served interceptions
   */
  public AndroidHttpClientResourceWrapper(final Context context, final HttpClient httpClient,
                                          final Map<String, Integer> urlToResourceIdMap,
                                          final Storage storage)
  {
    this.context = context;
    this.httpClient = httpClient;
    this.urlToResourceIdMap = Collections.synchronizedMap(urlToResourceIdMap);
    this.storage = storage;
  }

  public Listener getListener()
  {
    return listener;
  }

  public void setListener(Listener listener)
  {
    this.listener = listener;
  }

  @Override
  public void request(final HttpRequest request, final Callback callback)
  {
    // since parameters may vary we need to ignore them
    String urlWithoutParams = Utils.getUrlWithoutParams(request.getUrl());
    Integer resourceId = urlToResourceIdMap.get(urlWithoutParams);

    if (resourceId != null)
    {
      if (!storage.contains(urlWithoutParams))
      {
        Timber.w("Intercepting request for %s with resource #%d", request.getUrl(), resourceId);
        ServerResponse response = buildResourceContentResponse(resourceId);
        storage.put(urlWithoutParams);

        callback.onFinished(response);

        if (listener != null)
        {
          listener.onIntercepted(request.getUrl(), resourceId);
        }
        return;
      }
      else
      {
        Timber.d("Skip intercepting");
      }
    }

    // delegate to wrapper request
    httpClient.request(request, callback);
  }

  protected ByteBuffer readResourceContent(int resourceId) throws IOException
  {
    Timber.d("Reading from resource ...");

    InputStream is = null;

    try
    {
      is = context.getResources().openRawResource(resourceId);
      return Utils.readFromInputStream(is);
    }
    finally
    {
      if (is != null)
      {
        is.close();
      }
    }
  }

  protected ServerResponse buildResourceContentResponse(int resourceId)
  {
    ServerResponse response = new ServerResponse();
    try
    {
      response.setResponse(readResourceContent(resourceId));
      response.setResponseStatus(HttpClient.STATUS_CODE_OK);
      response.setStatus(ServerResponse.NsStatus.OK);
    }
    catch (IOException e)
    {
      Timber.e(e, "Error injecting response");
      response.setStatus(ServerResponse.NsStatus.ERROR_FAILURE);
    }

    return response;
  }

  /**
   * Listener for events
   */
  public interface Listener
  {
    void onIntercepted(String url, int resourceId);
  }

  /**
   * Interface to remember intercepted subscription requests
   */
  public interface Storage
  {
    void put(String url);
    boolean contains(String url);
  }

  /**
   * Storage impl in Shared Preferences
   */
  public static class SharedPrefsStorage implements Storage
  {
    private static final String URLS = "urls";

    private SharedPreferences prefs;
    private Set<String> urls;

    public SharedPrefsStorage(SharedPreferences prefs)
    {
      this.prefs = prefs;
      this.urls = prefs.getStringSet(URLS, new HashSet<String>());
    }

    @Override
    public synchronized void put(String url)
    {
      urls.add(url);

      prefs
        .edit()
        .putStringSet(URLS, urls)
        .commit();
    }

    @Override
    public synchronized boolean contains(String url)
    {
      return urls.contains(url);
    }
  }
}
