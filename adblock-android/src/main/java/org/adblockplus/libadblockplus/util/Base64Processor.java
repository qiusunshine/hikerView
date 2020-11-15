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

package org.adblockplus.libadblockplus.util;

public interface Base64Processor
{
  /**
   * Decode Base64 encoded bytes array
   * @param encodedBytes Base64 encoded bytes array
   * @return raw bytes array
   * @throws Base64Exception
   */
  byte[] decode(final byte[] encodedBytes) throws Base64Exception;

  /**
   * Encode raw bytes array to Base64
   * @param rawBytes raw bytes array
   * @return Base64 encoded bytes array
   * @throws Base64Exception
   */
  byte[] encode(final byte[] rawBytes) throws Base64Exception;

  /**
   * Encode raw bytes array to Base64 string
   * @param rawBytes raw bytes array
   * @return Base64 encoded string
   * @throws Base64Exception
   */
  String encodeToString(final byte[] rawBytes) throws Base64Exception;
}
