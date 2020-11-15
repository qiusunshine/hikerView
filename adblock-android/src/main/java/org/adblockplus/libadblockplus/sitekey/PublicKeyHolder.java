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

import java.util.List;

public interface PublicKeyHolder
{
  /**
   * Contains public key for URL
   * @param url URL
   * @return holder contains public key for URL
   */
  boolean contains(final String url);

  /**
   * Get public key for URL
   * @param url URL
   * @return public key Base64 encoded string or `null`
   */
  String get(final String url);

  /**
   * Get first public key for any of URLs in the list
   * @param urls URLs list
   * @param defaultValue default return value
   * @return first public key for any of URLs in the list
   */
  String getAny(final List<String> urls, final String defaultValue);

  /**
   * Put Base64 encoded public key string for URL
   * @param url URL
   * @param publicKey Base64 encoded public key string
   */
  void put(final String url, final String publicKey);

  /**
   * Clear map
   */
  void clear();
}
