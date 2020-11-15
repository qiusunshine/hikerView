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

package org.adblockplus.libadblockplus;

public class HeaderEntry
{
  private final String key;
  private final String value;

  public HeaderEntry(final String key, final String value)
  {
    this.key = key;
    this.value = value;
  }

  public static HeaderEntry of(final String key, final String value)
  {
    return new HeaderEntry(key, value);
  }

  public String getKey()
  {
    return this.key;
  }

  public String getValue()
  {
    return this.value;
  }

  @Override
  public int hashCode()
  {
    return this.key.hashCode() * 31 + this.value.hashCode();
  }

  @Override
  public boolean equals(final Object o)
  {
    if (!(o instanceof HeaderEntry))
    {
      return false;
    }
    final HeaderEntry other = (HeaderEntry) o;
    return this.key.equals(other.key) && this.value.equals(other.value);
  }

  @Override
  public String toString()
  {
    return this.key + ": " + this.value;
  }
}
