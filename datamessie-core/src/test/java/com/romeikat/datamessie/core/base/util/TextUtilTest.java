package com.romeikat.datamessie.core.base.util;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2017 Dr. Raphael Romeikat
 * =====================================================================
 * This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public
License along with this program.  If not, see
<http://www.gnu.org/licenses/gpl-3.0.html>.
 * =============================LICENSE_END=============================
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.core.base.util.TextUtil;

public class TextUtilTest extends AbstractTest {

  private static final String LOREM_IPSUM = "lorem ipsum dolor sit amet";

  @Autowired
  private TextUtil textUtil;

  @Test
  public void replaceAllAsWholeWord_beginning() {
    final String replaced = textUtil.replaceAllAsWholeWord(LOREM_IPSUM, "lorem", "FOO");
    assertEquals("FOO ipsum dolor sit amet", replaced);
  }

  @Test
  public void replaceAllAsWholeWord_middle() {
    final String replaced = textUtil.replaceAllAsWholeWord(LOREM_IPSUM, "dolor", "FOO");
    assertEquals("lorem ipsum FOO sit amet", replaced);
  }

  @Test
  public void replaceAllAsWholeWord_end() {
    final String replaced = textUtil.replaceAllAsWholeWord(LOREM_IPSUM, "amet", "FOO");
    assertEquals("lorem ipsum dolor sit FOO", replaced);
  }

  @Test
  public void replaceAllAsWholeWord_multiple() {
    final String replaced = textUtil.replaceAllAsWholeWord(LOREM_IPSUM, "ipsum dolor", "FOO");
    assertEquals("lorem FOO sit amet", replaced);
  }

  @Test
  public void replaceAllAsWholeWord_whole() {
    final String replaced = textUtil.replaceAllAsWholeWord(LOREM_IPSUM, LOREM_IPSUM, "FOO");
    assertEquals("FOO", replaced);
  }

  @Test
  public void replaceAllAsWholeWord_noWordBoundary() {
    final String replaced = textUtil.replaceAllAsWholeWord(LOREM_IPSUM, "lorem ips", "FOO");
    assertEquals(LOREM_IPSUM, replaced);
  }

  @Test
  public void replaceAllAsWholeWord_withMetacharacter() {
    final String replaced = textUtil.replaceAllAsWholeWord("this text contains metacharacters ???",
        "metacharacters ???", "no metacharacters");
    assertEquals("this text contains no metacharacters", replaced);
  }

}
