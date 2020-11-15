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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class FileSystemUtils
{
  /**
   * Delete file system entry (recursively if it's directory)
   * @param entry file or directory
   * @throws IOException
   */
  public static void delete(final File entry) throws IOException
  {
    if (entry.isDirectory())
    {
      File[] entries = entry.listFiles();
      if (entries != null)
      {
        for (File eachEntry : entries)
        {
          delete(eachEntry);
        }
      }
    }
    if (!entry.delete())
    {
      throw new IOException("Failed to delete file: " + entry);
    }
  }

  /**
   * Get file path relative to basePath
   * @param basePath base path
   * @param filename full absolute path
   * @return file path relative to basePath
   */
  public static String unresolve(final File basePath, final File filename)
  {
    return basePath.toURI().relativize(filename.toURI()).getPath();
  }
}
