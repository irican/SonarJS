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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.tree.statement.LabelledStatementTree;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;

@Rule(key = "S1219")
public class NonCaseLabelInSwitchCheck extends SubscriptionVisitorCheck {

  private static final String MESSAGE = "删除此误导性的 \"%s\" 标签";
  private Deque<Integer> stack = new ArrayDeque<>();

  @Override
  public Set<Kind> nodesToVisit() {
    return ImmutableSet.<Kind>builder()
      .add(Kind.LABELLED_STATEMENT, Kind.CASE_CLAUSE)
      .add(
        Kind.FUNCTION_EXPRESSION,
        Kind.FUNCTION_DECLARATION,
        Kind.GENERATOR_FUNCTION_EXPRESSION,
        Kind.GENERATOR_DECLARATION)
      .build();
  }

  @Override
  public void visitFile(Tree scriptTree) {
    stack.clear();
    stack.push(0);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.CASE_CLAUSE)) {
      enterCase();

    } else if (tree.is(Kind.LABELLED_STATEMENT)) {

      if (inCase()) {
        SyntaxToken label = ((LabelledStatementTree) tree).labelToken();
        addIssue(label, String.format(MESSAGE, label.text()));
      }

    } else {
      stack.push(0);
    }
  }


  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Kind.CASE_CLAUSE)) {
      leaveCase();

    } else if (tree.is(Kind.LABELLED_STATEMENT)) {
      // do nothing

    } else {
      stack.pop();
    }
  }

  private void leaveCase() {
    stack.push(stack.pop() - 1);
  }

  private boolean inCase() {
    return stack.peek() > 0;
  }

  private void enterCase() {
    stack.push(stack.pop() + 1);
  }

}
