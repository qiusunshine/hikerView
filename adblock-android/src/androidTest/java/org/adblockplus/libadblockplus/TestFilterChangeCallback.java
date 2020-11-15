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

import java.util.ArrayList;
import java.util.List;

public class TestFilterChangeCallback extends FilterChangeCallback
{
  private boolean called;
  private List<Event> events;

  public static class Event
  {
    private final String action;
    private final JsValue jsValue;

    public Event(final String action, final JsValue jsValue)
    {
      this.action = action;
      this.jsValue = jsValue;
    }

    public String getAction()
    {
      return action;
    }

    public JsValue getJsValue()
    {
      return jsValue;
    }

    @Override
    public String toString()
    {
      return action + (jsValue.isUndefined() ? "" : ": " + jsValue.toString());
    }
  }

  public TestFilterChangeCallback()
  {
    reset();
  }

  public boolean isCalled()
  {
    return called;
  }

  public List<Event> getEvents()
  {
    return events;
  }

  public void reset()
  {
    this.called = false;
    this.events = new ArrayList<>();
  }

  @Override
  public void filterChangeCallback(final String action, final JsValue jsValue)
  {
    this.called = true;
    this.events.add(new Event(action, jsValue));
  }
}
