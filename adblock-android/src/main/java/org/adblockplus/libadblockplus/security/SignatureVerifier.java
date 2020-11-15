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

package org.adblockplus.libadblockplus.security;

import java.security.PublicKey;

public interface SignatureVerifier
{
  /**
   * Verify value
   * @param publicKey public key
   * @param data raw data
   * @param signatureBytes raw signature data
   * @return value is verified
   * @throws SignatureVerificationException
   */
  boolean verify(final PublicKey publicKey, final byte[] data, final byte[] signatureBytes)
      throws SignatureVerificationException;
}
