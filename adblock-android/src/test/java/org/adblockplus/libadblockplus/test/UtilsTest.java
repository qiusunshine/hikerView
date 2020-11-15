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

import org.adblockplus.libadblockplus.android.Utils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilsTest
{
  @Test
  public void testAbsoluteUrl() throws URISyntaxException
  {
    assertFalse(Utils.isAbsoluteUrl("/index.html"));
    assertFalse(Utils.isAbsoluteUrl("../index.html"));
    assertFalse(Utils.isAbsoluteUrl("../../index.html"));

    assertTrue(Utils.isAbsoluteUrl("http://domain.com"));
    assertTrue(Utils.isAbsoluteUrl("https://domain.com"));
    assertTrue(Utils.isAbsoluteUrl("https://www.domain.com"));

    assertTrue(Utils.isAbsoluteUrl("https://www.domain.рф"));
  }

  @Test
  public void testRelativeUrl() throws MalformedURLException
  {
    final String relativeUrl = "/redirected.html";
    final String baseUrl = "http://domain.com";

    final String absoluteUrl = Utils.getAbsoluteUrl(baseUrl, relativeUrl);
    assertEquals(baseUrl + relativeUrl, absoluteUrl);
  }

  @Test
  public void testRelativeUrl_WithQuery() throws MalformedURLException
  {
    final String relativeUrl = "/redirected.html?argument=123";
    final String baseUrl = "http://domain.com";

    final String absoluteUrl = Utils.getAbsoluteUrl(baseUrl, relativeUrl);
    assertEquals(baseUrl + relativeUrl, absoluteUrl);
  }

  @Test
  public void testRelativeUrl_WithFragment() throws MalformedURLException
  {
    final String relativeUrl = "/redirected.html?argument=123#fragment";
    final String baseUrl = "http://domain.com";

    final String absoluteUrl = Utils.getAbsoluteUrl(baseUrl, relativeUrl);
    assertEquals(baseUrl + relativeUrl, absoluteUrl);
  }

  @Test
  public void testRelativeUrl_Https() throws MalformedURLException
  {
    final String relativeUrl = "/redirected.html?argument=123";
    final String baseUrl = "https://domain.com";

    final String absoluteUrl = Utils.getAbsoluteUrl(baseUrl, relativeUrl);
    assertEquals(baseUrl + relativeUrl, absoluteUrl);
  }

  @Test
  public void testExtractPathThrowsForInvalidUrl()
  {
    final String url = "some invalid url";
    try
    {
      Utils.extractPathWithQuery(url);
      fail("MalformedURLException is expected to be thrown");
    }
    catch (final MalformedURLException e)
    {
      // ignored
    }
  }

  @Test
  public void testExtractPathFromUrlsWithUnescapedCharacters() throws MalformedURLException
  {
    // "[" is illegal and throws URISyntaxException exception in `new URI(...)`
    final String url =
      "https://static-news.someurl.com/static-mcnews/2013/06/sbi-loan[1]_28825168_300x250.jpg";
    assertEquals("/static-mcnews/2013/06/sbi-loan[1]_28825168_300x250.jpg",
        Utils.extractPathWithQuery(url));
  }

  @Test
  public void testExtractPathWithoutQuery() throws MalformedURLException
  {
    final String url = "http://domain.com/image.jpeg";
    assertEquals("/image.jpeg", Utils.extractPathWithQuery(url));
  }

  @Test
  public void testExtractPathWithQuery() throws MalformedURLException
  {
    final String url = "http://domain.com/image.jpeg?id=5";
    assertEquals("/image.jpeg?id=5", Utils.extractPathWithQuery(url));
  }

  @Test
  public void testExtractPathWithoutQueryIgnoresFragment() throws MalformedURLException
  {
    final String url = "http://domain.com/image.jpeg#fragment";
    assertEquals("/image.jpeg", Utils.extractPathWithQuery(url));
  }

  @Test
  public void testExtractPathWithQueryIgnoresFragment() throws MalformedURLException
  {
    final String url = "http://domain.com/image.jpeg?id=5#fragment";
    assertEquals("/image.jpeg?id=5", Utils.extractPathWithQuery(url));
  }

  @Test
  public void testEscapeJavaScriptString()
  {
    assertEquals("name123", Utils.escapeJavaScriptString("name123"));       // nothing
    assertEquals("name\\\"123", Utils.escapeJavaScriptString("name\"123")); // "
    assertEquals("name\\'123", Utils.escapeJavaScriptString("name'123"));   // '
    assertEquals("name\\\\123", Utils.escapeJavaScriptString("name\\123")); // \
    assertEquals("name\\n123", Utils.escapeJavaScriptString("name\n123"));  // \n
    assertEquals("name\\r123", Utils.escapeJavaScriptString("name\r123"));  // \r
    assertEquals("123", Utils.escapeJavaScriptString(new String(new byte[]
        { '1', '2', '3' })));
    final Charset utf8 = Charset.forName("UTF-8");
    assertEquals("123\u202845", Utils.escapeJavaScriptString(new String(new byte[] //\u2028
        { '1', '2', '3', (byte)0xE2, (byte)0x80, (byte)0xA8, '4', '5' }, utf8)));
    assertEquals("123\u202945", Utils.escapeJavaScriptString(new String(new byte[] //\u2029
        { '1', '2', '3', (byte)0xE2, (byte)0x80, (byte)0xA9, '4', '5' }, utf8)));
  }

  @Test
  public void testGetDomain()
  {
    // Test success
    final Map<String, String> urlToDomainMap_OK = new HashMap<String, String>() {{
      put("http://domain.com:8080/", "domain.com");
      put("http://spl.abcdef.com/path?someparam=somevalue", "spl.abcdef.com");
      put("file://home/user/document.pdf", "home");
      put("data://text/vnd-example+xyz;foo=bar;base64,R0lGODdh", "text");

      put("http://spl.abcdef.com/path?someparam=somevalue", "spl.abcdef.com");
      put("file://home/user/document.pdf", "home");
      put("data://text/vnd-example+xyz;foo=bar;base64,R0lGODdh", "text");
    }};

    for (final Map.Entry<String, String> urlToDomainEntry : urlToDomainMap_OK.entrySet())
    {
      try
      {
        assertEquals(Utils.getDomain(urlToDomainEntry.getKey()), urlToDomainEntry.getValue());
      }
      catch (final URISyntaxException e)
      {
        fail(e.getMessage());
      }
    }

    // Test failures
    final List<String> wrongUrls = new ArrayList<String>() {{
      add("http://domain with spaces.com");
      add("www.unallowed%character.com");
      add("file://");
    }};

    for (final String urlEntry : wrongUrls)
    {
      try
      {
        Utils.getDomain(urlEntry);
        fail("URISyntaxException is expected to be thrown");
      }
      catch (final URISyntaxException e)
      {
        // expected
      }
    }
  }

  @Test
  public void testIsFirstPartyCookie()
  {
    final String navigationUrl = "https://some.domain.com/";

    // Test success
    final Map<String, String> urlAndCookieMap_OK = new HashMap<String, String>() {{
      put("https://some.domain.com/1", "somecookie=someValue; Path=/;");
      // "Domain" cookie parameter is used instead of a request url to obtain and compare domains
      put("https://blabla.com/2", "somecookie=someValue; Path=/; Domain=.domain.com");
      put("https://blabla.om/3", "somecookie=someValue; Path=/; Domain=some.domain.com");
    }};

    for (Map.Entry<String, String> urlAndCookieEntry : urlAndCookieMap_OK.entrySet())
    {
      assertTrue(Utils.isFirstPartyCookie(navigationUrl, urlAndCookieEntry.getKey(), urlAndCookieEntry.getValue()));
    }

    // Test failures
    final Map<String, String> urlAndCookieMap_NOK = new HashMap<String, String>() {{
      put("https://blabla.com/1", "somecookie=someValue; Path=/;");
      put("https://some.domain.com/2", "somecookie=someValue; Path=/; Domain=blabla.com");
      put("https://blabla.om/3", "somecookie=someValue; Path=/; Domain=www.some.domain.com");
    }};

    for (Map.Entry<String, String> urlAndCookieEntry : urlAndCookieMap_NOK.entrySet())
    {
      assertFalse(Utils.isFirstPartyCookie(navigationUrl, urlAndCookieEntry.getKey(), urlAndCookieEntry.getValue()));
    }
  }
}
