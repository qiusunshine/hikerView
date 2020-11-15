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

import org.adblockplus.libadblockplus.FileSystemUtils;
import org.adblockplus.libadblockplus.JsValue;
import org.adblockplus.libadblockplus.MockFileSystem;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FileSystemTest extends BaseJsEngineTest
{
  protected final MockFileSystem mockFileSystem = new MockFileSystem();

  // get file path relative to basePath
  protected String unresolve(final String filename)
  {
    return FileSystemUtils.unresolve(basePath, new File(filename));
  }

  @Override
  public void setUp()
  {
    setUpFileSystem(mockFileSystem);
    super.setUp();
  }

  private static class ReadResult
  {
    private JsValue content;
    private JsValue error;

    public ReadResult(final JsValue content, final JsValue error)
    {
      this.content = content;
      this.error = error;
    }

    public JsValue getContent()
    {
      return content;
    }

    public JsValue getError()
    {
      return error;
    }
  }

  private ReadResult readFile()
  {
    jsEngine.evaluate("let result = {}; _fileSystem.read('', function(r) {result.content = r.content;}, function(error) {result.error = error;})");
    final JsValue content = jsEngine.evaluate("result.content");
    final JsValue error = jsEngine.evaluate("result.error");
    return new ReadResult(content, error);
  }

  @Test
  public void testRead()
  {
    final String CONTENT = "foo";

    mockFileSystem.contentToRead = CONTENT;
    final ReadResult result = readFile();
    assertEquals(CONTENT, result.getContent().asString());
    assertTrue(result.getError().isUndefined());
  }

  @Test
  public void testReadError()
  {
    mockFileSystem.success = false;

    final ReadResult result = readFile();
    assertTrue(result.getContent().isUndefined());
    assertFalse(result.getError().isUndefined());
    assertNotNull(result.getError().asString());
  }

  @Test
  public void testReadException()
  {
    mockFileSystem.exception = true;

    final ReadResult result = readFile();
    assertTrue(result.getContent().isUndefined());
    assertFalse(result.getError().isUndefined());
    assertNotNull(result.getError().asString());
  }

  @Test
  public void testWrite()
  {
    jsEngine.evaluate("let error = true; _fileSystem.write('foo', 'bar', function(e) {error = e})");
    assertNotNull(mockFileSystem.lastWrittenFile);
    assertEquals("foo", unresolve(mockFileSystem.lastWrittenFile));
    assertNotNull(mockFileSystem.lastWrittenContent);
    assertEquals("bar", mockFileSystem.lastWrittenContent);
    assertTrue(jsEngine.evaluate("error").isUndefined());
  }

  @Test
  public void testWriteError()
  {
    mockFileSystem.success = false;

    jsEngine.evaluate("let error = true; _fileSystem.write('foo', 'bar', function(e) {error = e})");
    final JsValue error = jsEngine.evaluate("error");
    assertFalse(error.isUndefined());
    assertNotNull(error.asString());
  }

  @Test
  public void testWriteException()
  {
    mockFileSystem.exception = true;

    jsEngine.evaluate("let error = true; _fileSystem.write('foo', 'bar', function(e) {error = e})");
    final JsValue error = jsEngine.evaluate("error");
    assertFalse(error.isUndefined());
    assertNotNull(error.asString());
  }

  @Test
  public void testMove()
  {
    jsEngine.evaluate("let error = true; _fileSystem.move('foo', 'bar', function(e) {error = e})");
    assertNotNull(mockFileSystem.movedFrom);
    assertEquals("foo", unresolve(mockFileSystem.movedFrom));
    assertNotNull(mockFileSystem.movedTo);
    assertEquals("bar", unresolve(mockFileSystem.movedTo));
    assertTrue(jsEngine.evaluate("error").isUndefined());
  }

  @Test
  public void testMoveError()
  {
    mockFileSystem.success = false;

    jsEngine.evaluate("let error; _fileSystem.move('foo', 'bar', function(e) {error = e})");
    final JsValue error = jsEngine.evaluate("error");
    assertFalse(error.isUndefined());
    assertNotNull(error.asString());
  }

  @Test
  public void testMoveException()
  {
    mockFileSystem.exception = true;

    jsEngine.evaluate("let error; _fileSystem.move('foo', 'bar', function(e) {error = e})");
    final JsValue error = jsEngine.evaluate("error");
    assertFalse(error.isUndefined());
    assertNotNull(error.asString());
  }

  @Test
  public void testRemove()
  {
    jsEngine.evaluate("let error = true; _fileSystem.remove('foo', function(e) {error = e})");
    assertNotNull(mockFileSystem.removedFile);
    assertEquals("foo", unresolve(mockFileSystem.removedFile));
    assertTrue(jsEngine.evaluate("error").isUndefined());
  }

  @Test
  public void testRemoveError()
  {
    mockFileSystem.success = false;

    jsEngine.evaluate("let error = true; _fileSystem.remove('foo', function(e) {error = e})");
    final JsValue error = jsEngine.evaluate("error");
    assertFalse(error.isUndefined());
    assertNotNull(error.asString());
  }

  @Test
  public void testRemoveException()
  {
    mockFileSystem.exception = true;

    jsEngine.evaluate("let error = true; _fileSystem.remove('foo', function(e) {error = e})");
    final JsValue error = jsEngine.evaluate("error");
    assertFalse(error.isUndefined());
    assertNotNull(error.asString());
  }

  @Test
  public void testStat()
  {
    final boolean EXISTS = true;
    final long MODIFIED = 1337L;

    mockFileSystem.statExists = EXISTS;
    mockFileSystem.statLastModified = MODIFIED;
    jsEngine.evaluate("let result; _fileSystem.stat('foo', function(r) {result = r})");
    assertNotNull(mockFileSystem.statFile);
    assertEquals("foo", unresolve(mockFileSystem.statFile));
    assertTrue(jsEngine.evaluate("result.error").isUndefined());
    final JsValue exists = jsEngine.evaluate("result.exists");
    assertTrue(exists.isBoolean());
    assertEquals(EXISTS, exists.asBoolean());
    final JsValue modified = jsEngine.evaluate("result.lastModified");
    assertTrue(modified.isNumber());
    assertEquals(MODIFIED, modified.asLong());
  }

  @Test
  public void testStatError()
  {
    mockFileSystem.success = false;

    jsEngine.evaluate("let result; _fileSystem.stat('foo', function(r) {result = r})");
    final JsValue error = jsEngine.evaluate("result.error");
    assertFalse(error.isUndefined());
    assertNotNull(error.asString());
  }

  @Test
  public void testStatException()
  {
    mockFileSystem.exception = true;

    jsEngine.evaluate("let result; _fileSystem.stat('foo', function(r) {result = r})");
    final JsValue error = jsEngine.evaluate("result.error");
    assertFalse(error.isUndefined());
    assertNotNull(error.asString());
  }
}
