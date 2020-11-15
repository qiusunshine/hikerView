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

public class AppInfo
{
  public final String version;
  public final String name;
  public final String application;
  public final String applicationVersion;
  public final String locale;
  public final boolean developmentBuild;

  private AppInfo(final String version, final String name,
                  final String application, final String applicationVersion,
                  final String locale, final boolean developmentBuild)
  {
    this.version = version;
    this.name = name;
    this.application = application;
    this.applicationVersion = applicationVersion;
    this.locale = locale;
    this.developmentBuild = developmentBuild;
  }

  public static Builder builder()
  {
    return new Builder();
  }

  public static class Builder
  {
    private String version = "1.0";
    private String name = "libadblockplus-android";
    private String application = "android";
    private String applicationVersion = "0";
    private String locale = "en_US";
    private boolean developmentBuild = false;

    private Builder()
    {

    }

    public Builder setVersion(final String version)
    {
      this.version = version;
      return this;
    }

    public Builder setName(final String name)
    {
      this.name = name;
      return this;
    }

    public Builder setApplication(final String application)
    {
      this.application = application;
      return this;
    }

    public Builder setApplicationVersion(final String applicationVersion)
    {
      this.applicationVersion = applicationVersion;
      return this;
    }

    public Builder setLocale(final String locale)
    {
      this.locale = locale;
      return this;
    }

    public Builder setDevelopmentBuild(final boolean developmentBuild)
    {
      this.developmentBuild = developmentBuild;
      return this;
    }

    public AppInfo build()
    {
      return new AppInfo(this.version, this.name, this.application, this.applicationVersion, this.locale, this.developmentBuild);
    }
  }
}
