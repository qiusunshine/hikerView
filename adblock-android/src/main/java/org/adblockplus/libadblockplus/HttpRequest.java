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

import java.util.Collections;
import java.util.List;

public class HttpRequest
{
  /**
   * @param url Request URL.
   * @param method Request method (see REQUEST_METHOD_...).
   * @param headers Request headers.
   * @param followRedirect Enables/disables automatic redirection following
   * @param skipInputStreamReading Controls if HttpClient should read or just return input stream
   */
  public HttpRequest(final String url, final String method, final List<HeaderEntry> headers,
                     final boolean followRedirect, final boolean skipInputStreamReading)
  {
    this.url = url;
    this.method = method;
    this.headers = headers;
    this.followRedirect = followRedirect;
    this.skipInputStreamReading = skipInputStreamReading;
  }

  /**
   * Convenience ctor with GET request method and with no headers
   * @param url Request URL.
   */
  public HttpRequest(final String url)
  {
    this(url, HttpClient.REQUEST_METHOD_GET, Collections.<HeaderEntry>emptyList(), true, false);
  }

  private final String url;
  private final String method;
  private final List<HeaderEntry> headers;
  private final boolean followRedirect;
  private final boolean skipInputStreamReading;

  public String getUrl()
  {
    return url;
  }

  public String getMethod()
  {
    return method;
  }

  public List<HeaderEntry> getHeaders()
  {
    return headers;
  }

  public boolean getFollowRedirect()
  {
    return followRedirect;
  }

  public boolean skipInputStreamReading()
  {
    return skipInputStreamReading;
  }
}
