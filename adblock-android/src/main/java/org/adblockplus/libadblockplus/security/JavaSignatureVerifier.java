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

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * Implementation of SignatureVerified that uses Java Security API.
 * Signature is expected to be generated
 */
public class JavaSignatureVerifier implements SignatureVerifier
{
  public static final String KEY_ALGORITHM = "RSA";
  public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

  /**
   * Create KeySpec for public key bytes in DER format
   * @param publicKeyBytes public key bytes
   * @return KeySpec instance to be used in `verify` method.
   */
  public static PublicKey publicKeyFromDer(final String keyAlgorithm, final byte[] publicKeyBytes)
      throws SignatureVerificationException
  {
    try
    {
      final KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
      return keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }
    catch (Throwable cause)
    {
      throw new SignatureVerificationException(cause);
    }
  }

  private String signatureAlgorithm;

  public JavaSignatureVerifier(final String signatureAlgorithm)
  {
    this.signatureAlgorithm = signatureAlgorithm;
  }

  public JavaSignatureVerifier()
  {
    this(SIGNATURE_ALGORITHM);
  }

  @Override
  public boolean verify(final PublicKey publicKey, final byte[] data, final byte[] signatureBytes)
      throws SignatureVerificationException
  {
    try
    {
      final Signature signature = Signature.getInstance(this.signatureAlgorithm);
      signature.initVerify(publicKey);
      signature.update(data);
      return signature.verify(signatureBytes);
    }
    catch (Throwable cause)
    {
      throw new SignatureVerificationException(cause);
    }
  }
}
