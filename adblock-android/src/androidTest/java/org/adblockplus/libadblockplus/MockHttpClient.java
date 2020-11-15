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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MockHttpClient extends HttpClient
{
  public AtomicBoolean exception = new AtomicBoolean(false);
  public List<HttpRequest> requests = new LinkedList<>();
  public ServerResponse response;
  public AtomicBoolean called = new AtomicBoolean(false);

  public synchronized HttpRequest getSpecificRequest(final String url, final String method)
  {
    for (final HttpRequest request : requests)
    {
      if (request.getUrl().equals(url) && request.getMethod().equalsIgnoreCase(method))
      {
        return request;
      }
    }
    return null;
  }

  @Override
  public void request(final HttpRequest request, final Callback callback)
  {
    this.called.set(true);
    synchronized (this)
    {
      this.requests.add(request);
    }

    if (exception.get())
    {
      throw new RuntimeException("Exception simulation while downloading " + request.getUrl());
    }

    callback.onFinished(response);
  }
}
