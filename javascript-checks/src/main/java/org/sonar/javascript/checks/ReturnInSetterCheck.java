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

import org.sonar.check.Rule;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.declaration.AccessorMethodDeclarationTree;
import org.sonar.plugins.javascript.api.tree.statement.ReturnStatementTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;

@Rule(key = "S2432")
public class ReturnInSetterCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "请考虑删除此return语句; 它将被忽略。";

  private final DoubleDispatchVisitorCheck forbiddenReturnVisitor = new ForbiddenReturnVisitor();

  @Override
  public void visitAccessorMethodDeclaration(AccessorMethodDeclarationTree tree) {
    if (tree.is(Tree.Kind.SET_METHOD)) {
      tree.body().accept(forbiddenReturnVisitor);
    }
    super.visitAccessorMethodDeclaration(tree);
  }

  private class ForbiddenReturnVisitor extends DoubleDispatchVisitorCheck {

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      if (tree.expression() != null) {
        ReturnInSetterCheck check = ReturnInSetterCheck.this;
        check.addIssue(tree, MESSAGE);
      }
      super.visitReturnStatement(tree);
    }

  }

}
