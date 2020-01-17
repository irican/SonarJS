/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.javascript.checks;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.javascript.checks.utils.CheckUtils;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.plugins.javascript.api.visitors.FileIssue;
import org.sonar.plugins.javascript.api.visitors.JavaScriptFile;

@Rule(key = "S1451")
public class FileHeaderCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "添加或更新此文件的头文件";
  private static final String DEFAULT_HEADER_FORMAT = "";

  @RuleProperty(
    key = "headerFormat",
    description = "预期的版权和许可头",
    defaultValue = DEFAULT_HEADER_FORMAT,
    type = "TEXT")
  public String headerFormat = DEFAULT_HEADER_FORMAT;

  @RuleProperty(
    key = "isRegularExpression",
    description = "headerFormat 是否为正则表达式",
    defaultValue = "false")
  public boolean isRegularExpression = false;

  private String[] expectedLines = null;
  private Pattern searchPattern = null;

  @Override
  public void visitScript(ScriptTree tree) {
    if (isRegularExpression) {
      checkRegularExpression();

    } else {
      checkPlainText();
    }
  }

  private void checkPlainText() {
    if (expectedLines == null) {
      expectedLines = headerFormat.split("(?:\r)?\n|\r");
    }
    JavaScriptFile file = getContext().getJavaScriptFile();
    List<String> lines = CheckUtils.readLines(file);
    if (!matches(expectedLines, lines)) {
      addIssue(new FileIssue(this, MESSAGE));
    }
  }

  private void checkRegularExpression() {
    if (searchPattern == null) {
      try {
        searchPattern = Pattern.compile(headerFormat, Pattern.DOTALL);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("[" + getClass().getSimpleName() + "] 无法编译正则表达式: " + headerFormat, e);
      }
    }
    String fileContent;
    try {
      fileContent = getContext().getJavaScriptFile().contents();
    } catch (IOException e) {
      throw new IllegalStateException("无法读取文件 " + getContext().getJavaScriptFile().toString(), e);
    }

    Matcher matcher = searchPattern.matcher(fileContent);
    if (!matcher.find() || matcher.start() != 0) {
      addIssue(new FileIssue(this, MESSAGE));
    }
  }

  private static boolean matches(String[] expectedLines, List<String> lines) {
    boolean result;

    if (expectedLines.length <= lines.size()) {
      result = true;

      Iterator<String> it = lines.iterator();
      for (String expectedLine : expectedLines) {
        String line = it.next();
        if (!line.equals(expectedLine)) {
          result = false;
          break;
        }
      }
    } else {
      result = false;
    }

    return result;
  }

}
