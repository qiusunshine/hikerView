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

import org.adblockplus.libadblockplus.AppInfo;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

public class AppInfoTest
{
  @Test
  public void testAllProperties()
  {
    final String VERSION = "1";
    final String NAME = "3";
    final String APPLICATION = "4";
    final String APPLICATION_VERSION = "5";
    final String LOCALE = "2";
    final boolean DEVELOPMENT_BUILD = true;

    final AppInfo appInfo = AppInfo.builder()
        .setVersion(VERSION)
        .setName(NAME)
        .setApplication(APPLICATION)
        .setApplicationVersion(APPLICATION_VERSION)
        .setLocale(LOCALE)
        .setDevelopmentBuild(DEVELOPMENT_BUILD)
        .build();

    assertEquals(VERSION, appInfo.version);
    assertEquals(NAME, appInfo.name);
    assertEquals(APPLICATION, appInfo.application);
    assertEquals(APPLICATION_VERSION, appInfo.applicationVersion);
    assertEquals(LOCALE, appInfo.locale);
    assertEquals(DEVELOPMENT_BUILD, appInfo.developmentBuild);
  }

  @Test
  public void testDefaultPropertyValues()
  {
    final AppInfo appInfo = AppInfo.builder().build();

    assertEquals("1.0", appInfo.version);
    assertEquals("libadblockplus-android", appInfo.name);
    assertEquals("android", appInfo.application);
    assertEquals("0", appInfo.applicationVersion);
    assertEquals("en_US", appInfo.locale);
    assertFalse(appInfo.developmentBuild);
  }
}
