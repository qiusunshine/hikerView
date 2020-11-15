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

import timber.log.Timber;
import android.net.TrafficStats;

import org.adblockplus.libadblockplus.AdblockPlusException;
import org.adblockplus.libadblockplus.HeaderEntry;
import org.adblockplus.libadblockplus.HttpClient;
import org.adblockplus.libadblockplus.HttpRequest;
import org.adblockplus.libadblockplus.ServerResponse;
import org.adblockplus.libadblockplus.ServerResponse.NsStatus;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.adblockplus.libadblockplus.android.Utils.readFromInputStream;

public class AndroidHttpClient extends HttpClient
{
  protected static final String ENCODING_GZIP = "gzip";
  protected static final String ENCODING_IDENTITY = "identity";

  protected static final int SOCKET_TAG = 1;

  private final boolean compressedStream;
  private final String charsetName;

  /**
   * Ctor
   * @param compressedStream Request for gzip compressed stream from the server
   * @param charsetName Optional charset name for sending POST data
   */
  public AndroidHttpClient(final boolean compressedStream,
                           final String charsetName)
  {
    this.compressedStream = compressedStream;
    this.charsetName = charsetName;
  }

  public AndroidHttpClient()
  {
    this(true, "UTF-8");
  }

  @Override
  public void request(final HttpRequest request, final Callback callback)
  {
    if (!request.getMethod().equalsIgnoreCase(REQUEST_METHOD_GET))
    {
      throw new UnsupportedOperationException("Only GET method is supported");
    }

    final ServerResponse response = new ServerResponse();

    final int oldTag = TrafficStats.getThreadStatsTag();
    TrafficStats.setThreadStatsTag(SOCKET_TAG);
    Timber.d("Socket TAG set to: %s", SOCKET_TAG);

    HttpURLConnection connection = null;
    InputStream inputStream = null;
    try
    {
      final URL url = new URL(request.getUrl());
      Timber.d("Downloading from: %s, request.getFollowRedirect() = %b", url, request.getFollowRedirect());

      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(request.getMethod());

      if (request.getMethod().equalsIgnoreCase(REQUEST_METHOD_GET))
      {
        setGetRequestHeaders(request.getHeaders(), connection);
      }
      connection.setRequestProperty("Accept-Encoding",
        (compressedStream ? ENCODING_GZIP : ENCODING_IDENTITY));
      connection.setInstanceFollowRedirects(request.getFollowRedirect());

      Timber.d("Connecting...");
      connection.connect();
      Timber.d("Connected");

      if (connection.getHeaderFields().size() > 0)
      {
        Timber.d("Received header fields");

        List<HeaderEntry> responseHeaders = new LinkedList<>();
        for (Map.Entry<String, List<String>> eachEntry : connection.getHeaderFields().entrySet())
        {
          for (String eachValue : eachEntry.getValue())
          {
            if (eachEntry.getKey() != null && eachValue != null)
            {
              responseHeaders.add(new HeaderEntry(eachEntry.getKey().toLowerCase(), eachValue));
            }
          }
        }
        response.setResponseHeaders(responseHeaders);
      }
      try
      {
        final int responseStatus = connection.getResponseCode();
        response.setResponseStatus(responseStatus);
        response.setStatus(!isSuccessCode(responseStatus) ? NsStatus.ERROR_FAILURE : NsStatus.OK);

        Timber.d("responseStatus: %d for url %s", responseStatus, url);

        if (isSuccessCode(responseStatus))
        {
          Timber.d("Success responseStatus");
          inputStream = connection.getInputStream();
        }
        else
        {
          Timber.d("inputStream is set to Error stream");
          inputStream = connection.getErrorStream();
        }

        if (inputStream != null)
        {
          if (compressedStream && ENCODING_GZIP.equals(connection.getContentEncoding()))
          {
            Timber.d("Setting inputStream to GZIPInputStream");
            inputStream = new GZIPInputStream(inputStream);
          }

          /**
           * AndroidHttpClient is used by:
           * 1) Lower layer (JS core->C++->JNI->Java) and lower layer code expects that complete
           * response data is returned.
           * 2) Upper layer from WebViewClient.shouldInterceptRequest() (Java->Java) when we can
           * return just an InputStream allowing WebView to handle it (buffer or not).
           * To distinguish those two cases we are using now the new boolean argument in HttpRequest
           * constructor - `skipInputStreamReading`.
           * Later on we could switch just to returning InputStream for both cases but that would
           * require adaptations on lower layers (JNI/C++).
           */
          if (request.skipInputStreamReading())
          {
            Timber.d("response.setInputStream(inputStream)");
            // We need to do such a wrapping to let AdblockInputStream to call disconnect() on
            // connection when closing InputStream object. InputStream will be owned by WebView.
            inputStream = new ConnectionInputStream(inputStream, connection);
            response.setInputStream(inputStream);
          }
          else
          {
            Timber.d("readFromInputStream(inputStream)");
            response.setResponse(readFromInputStream(inputStream));
          }
        }
        else
        {
          Timber.w("inputStream is null");
        }

        if (!url.equals(connection.getURL()))
        {
          Timber.d("Url was redirected, from: %s, to: %s", url, connection.getURL());
          response.setFinalUrl(connection.getURL().toString());
        }
      }
      finally
      {
        if (!request.skipInputStreamReading() && (inputStream != null))
        {
          Timber.d("Closing connection input stream");
          inputStream.close();
        }
      }
      Timber.d("Downloading finished");
      callback.onFinished(response);
    }
    catch (final MalformedURLException e)
    {
      // MalformedURLException can be caused by wrong user input so we should not (re)throw it
      Timber.e(e, "WebRequest failed");
      response.setStatus(NsStatus.ERROR_MALFORMED_URI);
      callback.onFinished(response);
    }
    catch (final UnknownHostException e)
    {
      // UnknownHostException can be caused by wrong user input so we should not (re)throw it
      Timber.e(e, "WebRequest failed");
      response.setStatus(NsStatus.ERROR_UNKNOWN_HOST);
      callback.onFinished(response);
    }
    catch (final Throwable t)
    {
      Timber.e(t, "WebRequest failed");
      throw new AdblockPlusException("WebRequest failed", t);
    }
    finally
    {
      // when inputStream == null then connection won't be used anyway
      if (!request.skipInputStreamReading() || (inputStream == null))
      {
        if (connection != null)
        {
          connection.disconnect();
          Timber.d("Disconnected");
        }
      }
      TrafficStats.setThreadStatsTag(oldTag);
      Timber.d("Socket TAG reverted to: %d", oldTag);
    }
  }

  private void setGetRequestHeaders(final List<HeaderEntry> headers,
                                    final HttpURLConnection connection)
  {
    for (final HeaderEntry header : headers)
    {
      connection.setRequestProperty(header.getKey(), header.getValue());
    }
  }
}
