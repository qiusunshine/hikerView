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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import timber.log.Timber;

/**
 * InputStream wrapper that wraps `HttpURLConnection`s inputStream and closes wrapped connection
 * when it's input stream is closed.
 */
public class ConnectionInputStream extends InputStream
{
  private final InputStream inputStream;
  private final HttpURLConnection httpURLConnection;

  public ConnectionInputStream(final InputStream inputStream, final HttpURLConnection httpURLConnection)
  {
    this.inputStream = inputStream;
    this.httpURLConnection = httpURLConnection;
  }

  @Override
  public int read() throws IOException
  {
    return inputStream.read();
  }

  @Override
  public int read(byte b[]) throws IOException
  {
    return inputStream.read(b);
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException
  {
    return inputStream.read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException
  {
    return inputStream.skip(n);
  }

  @Override
  public int available() throws IOException
  {
    return inputStream.available();
  }

  @Override
  public void close() throws IOException
  {
    try
    {
      Timber.d("close()");
      inputStream.close();
    }
    finally
    {
      httpURLConnection.disconnect();
    }
  }

  @Override
  public synchronized void mark(int readlimit)
  {
    inputStream.mark(readlimit);
  }

  @Override
  public synchronized void reset() throws IOException
  {
    inputStream.reset();
  }

  @Override
  public boolean markSupported()
  {
    return inputStream.markSupported();
  }
}
