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

import java.util.List;

public class TestEventCallback extends EventCallback
{
  private boolean called;
  private List<JsValue> params;

  public TestEventCallback()
  {
    reset();
  }

  public boolean isCalled()
  {
    return called;
  }

  public List<JsValue> getParams()
  {
    return params;
  }

  public void reset()
  {
    this.called = false;
    this.params = null;
  }

  @Override
  public void eventCallback(final List<JsValue> params)
  {
    this.called = true;
    this.params = params;
  }
}
