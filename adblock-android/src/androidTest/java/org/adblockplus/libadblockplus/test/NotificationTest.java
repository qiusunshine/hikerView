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

import android.os.SystemClock;

import org.adblockplus.libadblockplus.Notification;
import org.adblockplus.libadblockplus.TestShowNotificationCallback;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class NotificationTest extends BaseFilterEngineTest
{
  private static final int NOTIFICATION_WAIT_DELAY_MS = 1000;

  protected void addNotification(final String notification)
  {
    jsEngine.evaluate(
        "(function()\n" +
        "{\n" +
        "require('notifications').notifications.addNotification(" + notification + ");\n" +
        "})();");
  }

  protected Notification peekNotification()
  {
    // Not using Mockito as `JniCallbackBase.javaVM` stays uninitialized causing NPE
    TestShowNotificationCallback callback = new TestShowNotificationCallback();
    filterEngine.setShowNotificationCallback(callback);
    filterEngine.showNextNotification();
    filterEngine.removeShowNotificationCallback();
    return callback.getNotification();
  }

  @Test
  public void testNoNotifications()
  {
    assertNull(peekNotification());
  }

  @Test
  public void testAddNotification()
  {
    addNotification(
        "{\n" +
        "   type: 'critical',\n" +
        "   title: 'testTitle',\n" +
        "   message: 'testMessage',\n" +
        "}");
    Notification notification = peekNotification();
    assertNotNull(notification);
    assertEquals("testTitle", notification.getTitle());
    assertEquals("testMessage", notification.getMessageString());
  }

  @Test
  public void testMarkAsShown()
  {
    addNotification("{ id: 'id', type: 'information' }");
    assertNotNull(peekNotification());

    Notification notification = peekNotification();
    assertNotNull(notification);

    SystemClock.sleep(NOTIFICATION_WAIT_DELAY_MS);
    notification.markAsShown();

    assertNull(peekNotification());
  }

  @Test
  public void testNoLinks()
  {
    addNotification("{ id: 'id'}");
    Notification notification = peekNotification();
    assertNotNull(notification);

    List<String> links = notification.getLinks();
    assertNotNull(links);
    assertEquals(0, links.size());
  }

  @Test
  public void testLinks()
  {
    addNotification("{ id: 'id', links: ['link1', 'link2'] }");
    Notification notification = peekNotification();
    assertNotNull(notification);

    List<String> links = notification.getLinks();
    assertNotNull(links);
    assertEquals(2, links.size());
    assertEquals("link1", links.get(0));
    assertEquals("link2", links.get(1));
  }
}
