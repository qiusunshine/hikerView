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

import org.adblockplus.libadblockplus.sitekey.PublicKeyHolder;
import org.adblockplus.libadblockplus.sitekey.PublicKeyHolderImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PublicKeyHolderImplTest
{
  private final Random random = new Random();
  private PublicKeyHolder publicKeyHolder;

  @Before
  public void setUp()
  {
    publicKeyHolder = new PublicKeyHolderImpl();
  }

  private String generateString()
  {
    return String.valueOf(Math.abs(random.nextLong()));
  }

  @Test
  public void testPutGet()
  {
    final String url = generateString();
    final String publicKey = generateString();

    assertFalse(publicKeyHolder.contains(url));
    publicKeyHolder.put(url, publicKey);
    assertTrue(publicKeyHolder.contains(url));
    assertEquals(publicKey, publicKeyHolder.get(url));
  }

  @Test
  public void testPutGetAny()
  {
    final String url = generateString();
    final String publicKey = generateString();

    assertFalse(publicKeyHolder.contains(url));
    publicKeyHolder.put(url, publicKey);
    assertTrue(publicKeyHolder.contains(url));

    final List<String> list = new ArrayList<>();

    assertNull(publicKeyHolder.getAny(list, null), null);
    assertEquals("", publicKeyHolder.getAny(list, ""));

    list.add(url);
    assertEquals(publicKey, publicKeyHolder.getAny(list, null));

    final String testUrlAfter = "testUrlAfter";
    list.add(testUrlAfter);
    assertFalse(publicKeyHolder.contains(testUrlAfter));
    assertEquals(publicKey, publicKeyHolder.getAny(list, null));

    final String testUrlBefore = "testUrlBefore";
    list.add(0, testUrlBefore);
    assertFalse(publicKeyHolder.contains(testUrlBefore));

    assertEquals(publicKey, publicKeyHolder.getAny(list, null));
  }

  @Test
  public void testGetAnyForTwo()
  {
    final String url1 = generateString();
    final String publicKey1 = generateString();
    assertFalse(publicKeyHolder.contains(url1));
    publicKeyHolder.put(url1, publicKey1);
    assertTrue(publicKeyHolder.contains(url1));

    final String url2 = generateString();
    final String publicKey2 = generateString();
    assertFalse(publicKeyHolder.contains(url2));
    publicKeyHolder.put(url2, publicKey2);
    assertTrue(publicKeyHolder.contains(url2));

    final List<String> list = new ArrayList<>();

    list.add(url1);
    assertEquals(publicKey1, publicKeyHolder.getAny(list, null));

    list.clear();
    list.add(url2);
    assertEquals(publicKey2, publicKeyHolder.getAny(list, null));

    // undefined behaviour (because map does not have order)
    list.add(url1);
    final String publicKey = publicKeyHolder.getAny(list, null);
    assertNotNull(publicKey);
    assertTrue(publicKey1.equals(publicKey) || publicKey2.equals(publicKey));
  }

  @Test
  public void testStripPadding()
  {
    final String publicKey = "somePublicKey";
    assertNull(PublicKeyHolderImpl.stripPadding(null));
    assertEquals("", PublicKeyHolderImpl.stripPadding(""));
    assertEquals(publicKey, PublicKeyHolderImpl.stripPadding(publicKey));
    assertEquals(publicKey, PublicKeyHolderImpl.stripPadding(publicKey + "="));
    assertEquals(publicKey, PublicKeyHolderImpl.stripPadding(publicKey + "=="));
  }
}
