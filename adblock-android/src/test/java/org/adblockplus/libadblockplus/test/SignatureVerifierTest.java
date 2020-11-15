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

package org.adblockplus.libadblockplus.test;

import org.adblockplus.libadblockplus.security.JavaSignatureVerifier;
import org.adblockplus.libadblockplus.security.SignatureVerificationException;
import org.adblockplus.libadblockplus.security.SignatureVerifier;
import org.junit.Before;
import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SignatureVerifierTest
{
  public static final int KEY_LENGTH_BITS = 512;
  public static final int VALID_SIGNATURE_LENGTH_BYTES = 64;

  private KeyPairGenerator keyGen;
  private SecureRandom random;
  private SignatureVerifier verifier;

  @Before
  public void setUp()
      throws NoSuchAlgorithmException, NoSuchProviderException
  {
    keyGen = KeyPairGenerator.getInstance(JavaSignatureVerifier.KEY_ALGORITHM);
    random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    keyGen.initialize(KEY_LENGTH_BITS, random);
    verifier = new JavaSignatureVerifier();
  }

  protected byte[] sign(final PrivateKey privateKey, final byte[] data)
      throws NoSuchAlgorithmException, InvalidKeyException, SignatureException
  {
    final Signature signature = Signature.getInstance(JavaSignatureVerifier.SIGNATURE_ALGORITHM);
    signature.initSign(privateKey);
    signature.update(data);
    return signature.sign();
  }

  protected byte[] generateData()
  {
    return String.valueOf(Math.abs(random.nextLong())).getBytes();
  }

  protected byte[] generateData(final int length)
  {
    final byte[] data = new byte[length];
    random.nextBytes(data);
    return data;
  }

  @Test
  public void testVerifySuccessfully()
      throws SignatureVerificationException,
          NoSuchAlgorithmException, InvalidKeyException, SignatureException
  {
    final KeyPair keyPair = keyGen.generateKeyPair();
    final byte[] data = generateData();
    final byte[] signature = sign(keyPair.getPrivate(), data);

    assertTrue(verifier.verify(keyPair.getPublic(), data, signature));
  }

  @Test
  public void testVerifyFailedBecauseOfWrongPublicKey()
      throws SignatureVerificationException,
          NoSuchAlgorithmException, InvalidKeyException, SignatureException
  {
    final KeyPair keyPair = keyGen.generateKeyPair();
    final byte[] data = generateData();
    final byte[] signature = sign(keyPair.getPrivate(), data);

    // trying to decrypt with public key that does not correspond to private key used to sign
    final KeyPair wrongKeys = keyGen.generateKeyPair();

    assertFalse(verifier.verify(wrongKeys.getPublic(), data, signature));
  }

  @Test
  public void testVerifyFailedBecauseOfWrongData()
      throws SignatureVerificationException,
          NoSuchAlgorithmException, InvalidKeyException, SignatureException
  {
    final KeyPair keyPair = keyGen.generateKeyPair();
    final byte[] data = generateData();
    final byte[] signature = sign(keyPair.getPrivate(), data);

    // trying to decrypt with public key for the data is different
    final byte[] wrongData = generateData();

    assertFalse(verifier.verify(keyPair.getPublic(), wrongData, signature));
  }

  @Test
  public void testVerifyFailedBecauseOfInvalidSignature()
  {
    final KeyPair keyPair = keyGen.generateKeyPair();
    final byte[] data = generateData();

    // trying to decrypt invalid signature (length is not equal to 64)
    final byte[] invalidSignature = generateData();
    assertTrue(invalidSignature.length != VALID_SIGNATURE_LENGTH_BYTES);
    final int actualSignatureLength = invalidSignature.length;

    try
    {
      verifier.verify(keyPair.getPublic(), data, invalidSignature);
      fail();
    }
    catch (SignatureVerificationException cause)
    {
      assertTrue(cause.getCause() instanceof SignatureException);
      // reference exception message is "Signature length not correct: got 19 but was expecting 64"
      // not comparing with exact string because of i18n
      final String causeMessage = cause.getCause().getMessage();
      assertTrue(causeMessage.contains(String.valueOf(VALID_SIGNATURE_LENGTH_BYTES)));
      assertTrue(causeMessage.contains(String.valueOf(actualSignatureLength)));
    }
  }

  @Test
  public void testVerifyFailedBecauseOfWrongSignature()
      throws SignatureVerificationException
  {
    final KeyPair keyPair = keyGen.generateKeyPair();
    final byte[] data = generateData();

    // trying to decrypt wrong signature
    final byte[] wrongSignature = generateData(64);
    assertEquals(VALID_SIGNATURE_LENGTH_BYTES, wrongSignature.length);

    assertFalse(verifier.verify(keyPair.getPublic(), data, wrongSignature));
  }
}
