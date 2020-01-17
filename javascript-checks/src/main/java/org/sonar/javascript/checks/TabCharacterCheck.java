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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.javascript.checks.utils.CheckUtils;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;
import org.sonar.plugins.javascript.api.visitors.JavaScriptFile;
import org.sonar.plugins.javascript.api.visitors.LineIssue;

@Rule(key = "TabCharacter")
public class TabCharacterCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "用空格序列替换此文件中的所有制表符。";

  @Override
  public void visitScript(ScriptTree tree) {
    JavaScriptFile javaScriptFile = getContext().getJavaScriptFile();
    List<String> lines = CheckUtils.readLines(javaScriptFile);

    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains("\t")) {
        addIssue(new LineIssue(this, i + 1, MESSAGE));
        break;
      }
    }

  }
}
