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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.plugins.javascript.api.JavaScriptCheck;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.visitors.Issue;
import org.sonar.plugins.javascript.api.visitors.LineIssue;
import org.sonar.plugins.javascript.api.visitors.PreciseIssue;
import org.sonar.plugins.javascript.api.visitors.TreeVisitorContext;

public abstract class EslintBasedCheck implements JavaScriptCheck {

  private static final IllegalStateException EXCEPTION = new IllegalStateException("不应为EslintBasedCheck创建问题");

  public abstract String eslintKey();

  public List<String> configurations() {
    return Collections.emptyList();
  }

  @Override
  public List<Issue> scanFile(TreeVisitorContext context) {
    return new ArrayList<>();
  }

  @Override
  public LineIssue addLineIssue(Tree tree, String message) {
    throw EXCEPTION;
  }

  @Override
  public PreciseIssue addIssue(Tree tree, String message) {
    throw EXCEPTION;
  }

  @Override
  public <T extends Issue> T addIssue(T issue) {
    throw EXCEPTION;
  }

  static List<String> configurations(int singleNumericParameter) {
    return Collections.unmodifiableList(Lists.newArrayList(String.valueOf(singleNumericParameter)));
  }

}
