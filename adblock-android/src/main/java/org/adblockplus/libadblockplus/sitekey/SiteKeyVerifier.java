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

import org.adblockplus.libadblockplus.security.JavaSignatureVerifier;
import org.adblockplus.libadblockplus.security.SignatureVerificationException;
import org.adblockplus.libadblockplus.security.SignatureVerifier;
import org.adblockplus.libadblockplus.util.Base64Exception;
import org.adblockplus.libadblockplus.util.Base64Processor;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;

public class SiteKeyVerifier
{
  private static final byte ZERO_BYTE = 0x0;
  private SignatureVerifier signatureVerifier;
  private PublicKeyHolder publicKeyHolder;
  private Base64Processor base64Processor;

  public SiteKeyVerifier(final SignatureVerifier signatureVerifier,
                         final PublicKeyHolder publicKeyHolder,
                         final Base64Processor base64Processor)
  {
    this.signatureVerifier = signatureVerifier;
    this.publicKeyHolder = publicKeyHolder;
    this.base64Processor = base64Processor;
  }

  /**
   * Verify 'X-Adblock-Key' value is valid
   * @param url url
   * @param userAgent user agent (`null` is accepted and processed as empty string)
   * @param value 'X-Adblock-Key' value
   * @return value is verified
   * @throws SiteKeyException
   */
  public boolean verify(final String url, final String userAgent, final String value)
      throws SiteKeyException
  {
    final String[] parts = value.split("_");
    if (parts.length != 2)
    {
      throw new SiteKeyException("Value is expected to be in format: " +
          "publicKey_signature, but actual parts count is " + parts.length);
    }

    final String publicKeyString = parts[0];
    final String signature = parts[1];

    final byte[] publicKeyBytes = decodeBase64(publicKeyString);
    final byte[] signatureBytes = decodeBase64(signature);
    final byte[] dataBytes = buildData(url, userAgent);

    try
    {
      final PublicKey publicKey = JavaSignatureVerifier.publicKeyFromDer(
          JavaSignatureVerifier.KEY_ALGORITHM, publicKeyBytes);

      if (signatureVerifier.verify(publicKey, dataBytes, signatureBytes))
      {
        publicKeyHolder.put(url, publicKeyString);
        return true;
      }
      return false;
    }
    catch (SignatureVerificationException e)
    {
      throw new SiteKeyException(e);
    }
  }

  private byte[] decodeBase64(final String encodedString) throws SiteKeyException
  {
    try
    {
      return base64Processor.decode(encodedString.getBytes());
    }
    catch (Base64Exception cause)
    {
      throw new SiteKeyException(cause);
    }
  }

  protected byte[] buildData(final String url, final String userAgent) throws SiteKeyException
  {
    final URI uri;
    try
    {
      uri = new URI(url);
      if (uri.getHost() == null)
        throw new URISyntaxException(url, "Can't extract host from URI");
    }
    catch (URISyntaxException cause)
    {
      throw new SiteKeyException(cause);
    }
    final String path = (uri.getPath() != null && uri.getPath().length() > 0 ? uri.getPath() : "/");
    final StringBuilder uriBuilder = new StringBuilder(path);
    if (uri.getQuery() != null)
    {
      uriBuilder.append("?");
      uriBuilder.append(uri.getRawQuery());
    }
    final byte[] urlBytes = uriBuilder.toString().getBytes();
    final byte[] hostBytes = uri.getAuthority().getBytes();
    final byte[] userAgentBytes = (userAgent != null ? userAgent.getBytes() : new byte[]{});

    final byte[] data = new byte[urlBytes.length + 1 + hostBytes.length + 1 + userAgentBytes.length];
    System.arraycopy(urlBytes, 0, data, 0, urlBytes.length);
    data[urlBytes.length] = ZERO_BYTE;
    System.arraycopy(hostBytes, 0, data, urlBytes.length + 1, hostBytes.length);
    data[urlBytes.length + 1 + hostBytes.length] = ZERO_BYTE;
    System.arraycopy(userAgentBytes, 0, data,
        urlBytes.length + 1 + hostBytes.length + 1, userAgentBytes.length);

    return data;
  }
}
