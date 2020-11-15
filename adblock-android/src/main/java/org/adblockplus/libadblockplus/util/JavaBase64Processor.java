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

import java.util.Base64;

/**
 * Java 8 implementation of Base64Processor
 */
public class JavaBase64Processor implements Base64Processor
{
  @Override
  public byte[] decode(final byte[] encodedBytes) throws Base64Exception
  {
    try
    {
      return Base64.getDecoder().decode(encodedBytes);
    }
    catch (Throwable cause)
    {
      throw new Base64Exception(cause);
    }
  }

  @Override
  public byte[] encode(final byte[] rawBytes) throws Base64Exception
  {
    try
    {
      return Base64.getEncoder().encode(rawBytes);
    }
    catch (Throwable cause)
    {
      throw new Base64Exception(cause);
    }
  }

  @Override
  public String encodeToString(final byte[] rawBytes) throws Base64Exception
  {
    try
    {
      return Base64.getEncoder().encodeToString(rawBytes);
    }
    catch (Throwable cause)
    {
      throw new Base64Exception(cause);
    }
  }
}
