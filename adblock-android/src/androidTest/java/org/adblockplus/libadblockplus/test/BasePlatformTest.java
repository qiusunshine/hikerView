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
 * GNU General Public License for more details.~
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.adblockplus.libadblockplus.test;

import org.adblockplus.libadblockplus.AppInfo;
import org.adblockplus.libadblockplus.FileSystem;
import org.adblockplus.libadblockplus.LogSystem;
import org.adblockplus.libadblockplus.MockHttpClient;
import org.adblockplus.libadblockplus.Platform;
import org.adblockplus.libadblockplus.HttpClient;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

public abstract class BasePlatformTest extends BaseTest
{
  protected SetUpInfo setUpInfo = new SetUpInfo();
  protected Platform platform;

  static class SetUpInfo
  {
    AppInfo appInfo = AppInfo.builder().build();
    LogSystem logSystem = BasePlatformTest.buildNoopLogSystem();
    FileSystem fileSystem = null; // using Default implementation in C++
    HttpClient httpClient = BasePlatformTest.buildThrowingHttpClient();
  }

  public static Throwable buildThrowable()
  {
    return new RuntimeException("Throwable implementation");
  }

  public static LogSystem buildThrowingLogSystem()
  {
    LogSystem throwingLogSystem = mock(LogSystem.class);
    Mockito
        .doThrow(buildThrowable())
        .when(throwingLogSystem)
        .logCallback(Mockito.<LogSystem.LogLevel>any(), anyString(), anyString());
    return throwingLogSystem;
  }

  public static LogSystem buildNoopLogSystem()
  {
    return mock(LogSystem.class);
  }

  public static HttpClient buildThrowingHttpClient()
  {
    MockHttpClient throwingHttpClient = new MockHttpClient();
    throwingHttpClient.exception.set(true);
    return throwingHttpClient;
  }

  protected void setUpAppInfo(AppInfo appInfo)
  {
    setUpInfo.appInfo = appInfo;
  }

  protected void setUpLogSystem(LogSystem logSystem)
  {
    setUpInfo.logSystem = logSystem;
  }

  protected void setUpFileSystem(FileSystem fileSystem)
  {
    setUpInfo.fileSystem = fileSystem;
  }

  protected  void setUpHttpClient(HttpClient httpClient)
  {
    setUpInfo.httpClient = httpClient;
  }

  private void setUpPlatform()
  {
    platform = new Platform(setUpInfo.logSystem, setUpInfo.fileSystem, setUpInfo.httpClient, basePath.getAbsolutePath());
  }

  @Override
  public void setUp()
  {
    super.setUp();
    setUpPlatform();
  }

  private void tearDownPlatform()
  {
    if (platform == null)
    {
      return;
    }

    platform.dispose();
    platform = null;
  }

  @Override
  public void tearDown()
  {
    tearDownPlatform();
    super.tearDown();
  }
}
