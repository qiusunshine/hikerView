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
import org.adblockplus.libadblockplus.security.SignatureVerifier;
import org.adblockplus.libadblockplus.sitekey.PublicKeyHolder;
import org.adblockplus.libadblockplus.sitekey.PublicKeyHolderImpl;
import org.adblockplus.libadblockplus.sitekey.SiteKeyException;
import org.adblockplus.libadblockplus.sitekey.SiteKeyVerifier;
import org.adblockplus.libadblockplus.util.Base64Exception;
import org.adblockplus.libadblockplus.util.Base64Processor;
import org.adblockplus.libadblockplus.util.JavaBase64Processor;
import org.junit.Test;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SiteKeyVerifierTest
{
  private final SignatureVerifier signatureVerifier = new JavaSignatureVerifier();
  private final PublicKeyHolder publicKeyHolder = new PublicKeyHolderImpl();
  private final Base64Processor base64Processor = new JavaBase64Processor();
  private final TestSiteKeyVerifier siteKeyVerifier =
      new TestSiteKeyVerifier(signatureVerifier, publicKeyHolder, base64Processor);

  private static class TestSiteKeyVerifier extends SiteKeyVerifier
  {
    public TestSiteKeyVerifier(final SignatureVerifier signatureVerifier,
                               final PublicKeyHolder publicKeyHolder,
                               final Base64Processor base64Processor)
    {
      super(signatureVerifier, publicKeyHolder, base64Processor);
    }

    @Override
    // exposed method as `public`
    public byte[] buildData(String url, String userAgent) throws SiteKeyException
    {
      return super.buildData(url, userAgent);
    }
  }

  protected void assertVerified(final String url, final String userAgent, final String value)
      throws SiteKeyException
  {
    assertTrue(siteKeyVerifier.verify(url, userAgent, value));
    final String actualPublicKey = publicKeyHolder.get(url);
    assertNotNull(actualPublicKey);
  }

  protected String buildXAdblockKeyValue(final String url, final String userAgent)
      throws SiteKeyException, Base64Exception,
      NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
  {
    final byte[] data = siteKeyVerifier.buildData(url, userAgent);
    final int KEY_LENGTH_BITS = 512;

    // generate key pair
    final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(JavaSignatureVerifier.KEY_ALGORITHM);
    final SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    keyGen.initialize(KEY_LENGTH_BITS, random);
    final KeyPair keyPair = keyGen.generateKeyPair();

    // sign and decode Base64 data
    final Signature signature = Signature.getInstance(JavaSignatureVerifier.SIGNATURE_ALGORITHM);
    signature.initSign(keyPair.getPrivate());
    signature.update(data);
    final byte[] signatureBytes = signature.sign();
    final String decodedSignature = base64Processor.encodeToString(signatureBytes);

    // decode to Base64 public key
    final byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
    final String decodedPublicKey = base64Processor.encodeToString(publicKeyBytes);
    return decodedPublicKey + "_" + decodedSignature;
  }

  protected void signAndAssertVerified(final String signatureUrl, final String verifyUrl,
                                       final String userAgent) throws Exception
  {
    final String value = buildXAdblockKeyValue(signatureUrl, userAgent);
    assertVerified(verifyUrl, userAgent, value);
  }

  protected void signAndAssertVerified(final String url, final String userAgent) throws Exception
  {
    signAndAssertVerified(url, url, userAgent);
  }

  @Test
  public void testBuildData() throws SiteKeyException
  {
    final String url = "http://www.example.com/index.html?q=foo";
    final String userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:30.0) Gecko/20100101 Firefox/30.0";

    final TestSiteKeyVerifier siteKeyVerifier = new TestSiteKeyVerifier(
        signatureVerifier, publicKeyHolder, base64Processor);
    final byte[] dataBytes = siteKeyVerifier.buildData(url, userAgent);
    assertNotNull(dataBytes);
    assertEquals(110, dataBytes.length);
  }

  @Test
  public void testVerifySuccessfully() throws SiteKeyException
  {
    final String url = "https://www.cook.com/";
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAL/3/SrV7P8AsTHMFSpPmYbyv2PkACHwmG9Z+1IFZq3vA54IN7pQcGnhgNo+8SN9r/KtUWCb9OPqTfWM1N4w/EUCAwEAAQ==_aW33lgRJlrkr/OXJpTwWfOhcsYG1vCMggeNdI1CoMtGSkeCqYYmmJ5SYTCIH4GVRfP6UiteZGlNJDCPs76b8Mw==";
    assertVerified(url, userAgent, value);
  }

  @Test
  public void testVerifySuccessfullyWithoutTrailingSlash() throws SiteKeyException
  {
    final String url = "https://www.cook.com"; // there is no traling '/' in url
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAL/3/SrV7P8AsTHMFSpPmYbyv2PkACHwmG9Z+1IFZq3vA54IN7pQcGnhgNo+8SN9r/KtUWCb9OPqTfWM1N4w/EUCAwEAAQ==_aW33lgRJlrkr/OXJpTwWfOhcsYG1vCMggeNdI1CoMtGSkeCqYYmmJ5SYTCIH4GVRfP6UiteZGlNJDCPs76b8Mw==";
    assertVerified(url, userAgent, value);
  }

  @Test
  public void testVerifySuccessfullyAndroidUserAgent() throws SiteKeyException
  {
    final String url = "https://www.cook.com/";
    final String userAgent = "Mozilla/5.0 (Linux; Android 5.0; SM-N9005 Build/LRX21V; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 Mobile Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAL/3/SrV7P8AsTHMFSpPmYbyv2PkACHwmG9Z+1IFZq3vA54IN7pQcGnhgNo+8SN9r/KtUWCb9OPqTfWM1N4w/EUCAwEAAQ==_kEg19pZTbDr0ZECe0Nk33QCo5XDOU8UijTiD4VE4fRQ9Spn2DyYiLsdMWmz9uKIpxs+gyrgGPEru21T9khTvzw==";
    assertVerified(url, userAgent, value);
  }

  @Test
  public void testVerifySuccessfullyCablemovers() throws SiteKeyException
  {
    final String url = "http://ww7.cablemovers.com/?z";
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANDrp2lz7AOmADaN8tA50LsWcjLFyQFcb/P2Txc58oYOeILb3vBw7J6f4pamkAQVSQuqYsKx3YzdUHCvbVZvFUsCAwEAAQ==_puuixp134cZfd3ne6I40yYgYav0NFv0IJ4IRgrvVxBpKgDjuGjLYapLwHHrAgfJmzjUJAck2Vb6THr2b92CoyQ==";
    assertVerified(url, userAgent, value);
  }

  @Test
  public void testVerifySuccessfullyUrlWithPort() throws Exception
  {
    final String url = "https://tlucas-1.custom.eyeo.it:8433/sitekey-frame";
    final String userAgent = "Mozilla/5.0 (Linux; Android 9; Android SDK built for x86 Build/PSR1.180720.075; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.157 Mobile Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANGtTstne7e8MbmDHDiMFkGbcuBgXmiVesGOG3gtYeM1EkrzVhBjGUvKXYE4GLFwqty3v5MuWWbvItUWBTYoVVsCAwEAAQ==_xTrHxAMoL8zruHvNeDo4zaJBQj7zfpzel7COfLTcjJqjccwlBA9ahXhaJ+a4NR1srWbE0ONYJfxN+Z0O3tqzVA==";
    assertVerified(url, userAgent, value);
  }

  @Test
  public void testVerifySuccessfullyUrlWithComplexQuery() throws Exception
  {
    final String url = "http://demo.aaxdemo.com/display.html?&_otarOg=http%3A%2F%2Faaxdemo.com&_cpub=AAXSFY9XU&_csvr=2019052905_3022&_cgdpr=0&_cgdprconsent=0";
    final String userAgent = "Mozilla/5.0 (Linux; Android 9; Android SDK built for x86 Build/PSR1.180720.075; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.157 Mobile Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMLlOP3Rke738aeqtDCGp0IgSY5XBv7c+brDMmurbYvOFgakGw6sUG8fwt6VkjnOX9s9Kba1Drg2M9Bye/F3x7MCAwEAAQ==_KeG8qinJIkcjfinTUBViCW/mmk/WnjPm2Bz+OTD0ffqBmdo31us7KP4vXyfXoiKVfmCxeKNmYO09u+kYlI4wLw==";
    assertVerified(url, userAgent, value);
  }

  @Test
  public void testVerifySuccessfullyNonEnglish() throws Exception
  {
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";

    signAndAssertVerified("http://www.domain.com", userAgent);
    signAndAssertVerified("http://xn--h1alffa9f.xn--p1ai/", userAgent); // url="http://россия.рф"
  }

  @Test
  public void testVerifySuccessfullyNoUserAgent() throws Exception
  {
    final String userAgent = null; // null is accepted and processed as empty string
    signAndAssertVerified("http://www.domain.com", userAgent);
  }

  @Test
  public void testVerifySuccessfullyWithFragment() throws Exception
  {
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";

    signAndAssertVerified("http://www.domain.com?query=123#fragment1", "http://www.domain.com?query=123", userAgent);
    signAndAssertVerified("http://www.domain.com#fragment1", "http://www.domain.com", userAgent);
  }

  @Test
  public void testVerifyFailedNot2Parts()
  {
    final String url = "http://www.domain.com";
    final String userAgent = "user agent";
    final String value = "value"; // should be in key_agent

    try
    {
      assertVerified(url, userAgent, value);
      fail();
    }
    catch (SiteKeyException cause)
    {
      assertNotNull(cause);
    }
  }

  protected void assertVerifyFailedInvalidUrl(
      final String url, final String userAgent, final String value)
  {
    try
    {
      siteKeyVerifier.verify(url, userAgent, value);
      fail();
    }
    catch (SiteKeyException cause)
    {
      assertNotNull(cause);
      assertTrue(cause.getCause() instanceof URISyntaxException);
    }
  }

  @Test
  public void testVerifyFailedInvalidUrl()
  {
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANDrp2lz7AOmADaN8tA50LsWcjLFyQFcb/P2Txc58oYOeILb3vBw7J6f4pamkAQVSQuqYsKx3YzdUHCvbVZvFUsCAwEAAQ==_puuixp134cZfd3ne6I40yYgYav0NFv0IJ4IRgrvVxBpKgDjuGjLYapLwHHrAgfJmzjUJAck2Vb6THr2b92CoyQ==";

    final String invalidUrl = "someInvalidString";

    assertVerifyFailedInvalidUrl(invalidUrl, userAgent, value);
  }

  @Test
  public void testVerifyFailedInvalidUrlNoHost()
  {
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANDrp2lz7AOmADaN8tA50LsWcjLFyQFcb/P2Txc58oYOeILb3vBw7J6f4pamkAQVSQuqYsKx3YzdUHCvbVZvFUsCAwEAAQ==_puuixp134cZfd3ne6I40yYgYav0NFv0IJ4IRgrvVxBpKgDjuGjLYapLwHHrAgfJmzjUJAck2Vb6THr2b92CoyQ==";

    final String invalidUrl = "http://"; // no host

    assertVerifyFailedInvalidUrl(invalidUrl, userAgent, value);
  }

  @Test
  public void testVerifyFailedWrongUrl() throws SiteKeyException
  {
    final String url = "http://ww7.cablemovers.com/?z";
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANDrp2lz7AOmADaN8tA50LsWcjLFyQFcb/P2Txc58oYOeILb3vBw7J6f4pamkAQVSQuqYsKx3YzdUHCvbVZvFUsCAwEAAQ==_puuixp134cZfd3ne6I40yYgYav0NFv0IJ4IRgrvVxBpKgDjuGjLYapLwHHrAgfJmzjUJAck2Vb6THr2b92CoyQ==";

    final String wrongUrl = url.replace("a", "b");
    assertNotEquals(url, wrongUrl);

    assertFalse(siteKeyVerifier.verify(wrongUrl, userAgent, value));
  }

  @Test
  public void testVerifyFailedWrongUserAgent() throws SiteKeyException
  {
    final String url = "http://ww7.cablemovers.com/?z";
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANDrp2lz7AOmADaN8tA50LsWcjLFyQFcb/P2Txc58oYOeILb3vBw7J6f4pamkAQVSQuqYsKx3YzdUHCvbVZvFUsCAwEAAQ==_puuixp134cZfd3ne6I40yYgYav0NFv0IJ4IRgrvVxBpKgDjuGjLYapLwHHrAgfJmzjUJAck2Vb6THr2b92CoyQ==";

    final String wrongUserAgent = userAgent.replace("a", "b");
    assertNotEquals(userAgent, wrongUserAgent);

    assertFalse(siteKeyVerifier.verify(url, wrongUserAgent, value));
  }

  @Test
  public void testVerifyFailedWrongSignature() throws SiteKeyException
  {
    final String url = "http://ww7.cablemovers.com/?z";
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANDrp2lz7AOmADaN8tA50LsWcjLFyQFcb/P2Txc58oYOeILb3vBw7J6f4pamkAQVSQuqYsKx3YzdUHCvbVZvFUsCAwEAAQ==_puuixp134cZfd3ne6I40yYgYav0NFv0IJ4IRgrvVxBpKgDjuGjLYapLwHHrAgfJmzjUJAck2Vb6THr2b92CoyQ==";

    final StringBuilder wrongValueBuilder = new StringBuilder(value);
    final int length = wrongValueBuilder.length();
    final String wrongValue = wrongValueBuilder
        .replace( length - 3, length - 2, "D")
        .toString();
    assertNotEquals(value, wrongValue);

    assertFalse(siteKeyVerifier.verify(url, userAgent, wrongValue));
  }

  @Test
  public void testUwe() throws SiteKeyException
  {
    final String url = "https://bernitt.biz/sitekeys/whitelisted.php";
    final String userAgent = "Mozilla/5.0 (Linux; Android 5.0; SM-N9005 Build/LRX21V; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 Mobile Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAM2wIdWGjAKRRHWAbD2TfzrzAruuA9tmCD0ovLvZqsdHjQ8xX18+k8VS3D4NKO8yzXca6e86USA13bBAFq8MXZkCAwEAAQ==_sMBhYhXWLNDS2hLeeGnisubbhF0UKqkTd+VlNk/T/C8wQ9NkYX6TR8421n/+bcjC7W5lK4ta1U9ghQJmCV8jgQ==";

    assertVerified(url, userAgent, value);
  }

  @Test
  public void testUwe2() throws SiteKeyException
  {
    final String url = "https://bernitt.biz/sitekeys/whitelisted.php";
    final String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    final String value = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAM2wIdWGjAKRRHWAbD2TfzrzAruuA9tmCD0ovLvZqsdHjQ8xX18+k8VS3D4NKO8yzXca6e86USA13bBAFq8MXZkCAwEAAQ==_Xj0zwDvJJ0RLa1VaePPDhXcDURBMEdO59XxsOc1BmEFByd9UQMGVlQ59WxyESToPY6qVZXJFdEERKrRUe/fnrg==";

    assertVerified(url, userAgent, value);
  }
}
