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

import org.adblockplus.libadblockplus.android.AndroidBase64Processor;
import org.adblockplus.libadblockplus.util.Base64Exception;
import org.adblockplus.libadblockplus.util.Base64Processor;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AndroidBase64ProcessorTest
{
  private final static String STRING = "Hello, world";
  private final static byte[] INVALID_BYTES =
      { -93, -91, -25, 25, -87, 121, 77, -71, -67, 119, -68, 61, -4, 112, -128 };
  private final Random random = new Random();
  private final Base64Processor base64Processor = new AndroidBase64Processor();

  private String generateString()
  {
    return String.valueOf(Math.abs(random.nextLong()));
  }

  @Test
  public void testEncodeSuccessfully() throws Base64Exception
  {
    final byte[] encodedBytes = base64Processor.encode(STRING.getBytes());
    assertNotNull(encodedBytes);
    assertTrue(encodedBytes.length > 0);
    final String encodedString = base64Processor.encodeToString(STRING.getBytes());
    assertNotNull(encodedString);
    assertTrue(Arrays.equals(encodedBytes, encodedString.getBytes()));
  }

  @Test
  public void testEncodeEmptyString() throws Base64Exception
  {
    final byte[] encoded = base64Processor.encode("".getBytes());
    assertNotNull(encoded);
    assertEquals(0, encoded.length);
  }

  @Test
  public void testDecodeSuccessfully() throws Base64Exception
  {
    final byte[] decodedString = "SGVsbG8sIHdvcmxk".getBytes();
    final byte[] actualStringBytes = base64Processor.decode(decodedString);
    assertNotNull(actualStringBytes);
    assertTrue(Arrays.equals(STRING.getBytes(), actualStringBytes));
  }

  @Test
  public void testDecodeFailed()
  {
    try
    {
      base64Processor.decode(INVALID_BYTES);
      fail();
    }
    catch (Base64Exception cause)
    {
      assertTrue(cause.getCause() instanceof IllegalArgumentException);
    }
  }

  @Test
  public void testEncodeDecode() throws Base64Exception
  {
    final byte[] randomStringBytes = generateString().getBytes();
    final byte[] encoded = base64Processor.encode(randomStringBytes);
    final byte[] decoded = base64Processor.decode(encoded);
    assertTrue(Arrays.equals(randomStringBytes, decoded));
  }

  @Test
  public void testDecodeEmpty() throws Base64Exception
  {
    final byte[] empty = {};
    final byte[] decoded = base64Processor.decode(empty);
    assertNotNull(decoded);
    assertEquals(0, decoded.length);
  }

  @Test
  public void testEncodeDecodeNonEnglish() throws Base64Exception
  {
    final byte[] expected = "Привет, мир".getBytes();
    final byte[] encoded = base64Processor.encode(expected);
    final byte[] actual = base64Processor.decode(encoded);
    assertNotNull(actual);
    assertTrue(Arrays.equals(expected, actual));
  }

  @Test
  public void testEncodeDecodeUTF() throws Base64Exception
  {
    final Charset charset = Charset.forName("UTF-8");
    final byte[] expected = "\u0075\u0090".getBytes(charset);
    final byte[] encoded = base64Processor.encode(expected);
    final byte[] actual = base64Processor.decode(encoded);
    assertNotNull(actual);
    assertTrue(Arrays.equals(expected, actual));
  }

  @Test
  public void testEncodeDecodeNewLine() throws Base64Exception
  {
    final byte[] expected = "Hello\nworld".getBytes();
    final byte[] encoded = base64Processor.encode(expected);
    final byte[] actual = base64Processor.decode(encoded);
    assertNotNull(actual);
    assertTrue(Arrays.equals(expected, actual));
  }
}
