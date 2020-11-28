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

package org.adblockplus.libadblockplus.android.settings;

import android.content.Context;

import org.junit.Test;

import java.util.Map;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UtilsTest
{
  @Test
  public void testLocalesTitles()
  {
    final Context context = ApplicationProvider.getApplicationContext();
    final Map<String, String> localeToTitle = Utils.getLocaleToTitleMap(context);
    assertNotNull(localeToTitle);
    assertTrue(localeToTitle.size() > 0);
    final String actualEnglishTitle = localeToTitle.get("en");
    assertTrue("English".equalsIgnoreCase(actualEnglishTitle));
  }
}
