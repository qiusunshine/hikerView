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

package org.adblockplus.libadblockplus.sitekey;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe implementation of PublicKeyHolder
 */
public class PublicKeyHolderImpl implements PublicKeyHolder
{
  /**
   * Strip `=` padding at the end of base64 public key
   * @param publicKey full public key
   * @return public key with stripped padding or `null`
   */
  public static String stripPadding(final String publicKey)
  {
    if (publicKey == null)
    {
      return null;
    }

    final StringBuilder sb = new StringBuilder(publicKey);
    while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '=')
    {
      sb.deleteCharAt(sb.length() - 1);
    }
    return sb.toString();
  }

  private Map<String, String> map = Collections.synchronizedMap(new HashMap<String, String>());

  @Override
  public boolean contains(final String url)
  {
    return map.containsKey(url);
  }

  @Override
  public String get(final String url)
  {
    return map.get(url);
  }

  @Override
  public String getAny(final List<String> urls, final String defaultValue)
  {
    for (final String url : urls)
    {
      final String publicKey = get(url);
      if (publicKey != null)
      {
        return publicKey;
      }
    }
    return defaultValue;
  }

  @Override
  public void put(final String url, final String publicKey)
  {
    map.put(url, publicKey);
  }

  @Override
  public void clear()
  {
    map.clear();
  }
}
