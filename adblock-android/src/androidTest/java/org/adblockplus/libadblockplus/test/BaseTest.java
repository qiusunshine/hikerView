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

import android.content.Context;

import org.adblockplus.libadblockplus.FileSystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import androidx.test.core.app.ApplicationProvider;

import timber.log.Timber;

public abstract class BaseTest
{
  protected File basePath;

  private void setUpBasePath()
  {
    basePath = new File(ApplicationProvider.getApplicationContext()
        .getDir("test" + Math.abs(new Random().nextLong()), Context.MODE_PRIVATE)
        .getAbsolutePath());
    basePath.mkdirs();
  }

  @BeforeClass
  public static void beforeClass()
  {
    if (BuildConfig.DEBUG)
    {
      Timber.plant(new Timber.DebugTree());
    }
  }

  @Before
  public void setUp()
  {
    setUpBasePath();
  }

  private void tearDownBasePath()
  {
    if (basePath == null)
    {
      return;
    }

    try
    {
      FileSystemUtils.delete(basePath);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    basePath = null;
  }

  @After
  public void tearDown()
  {
    tearDownBasePath();
  }
}
