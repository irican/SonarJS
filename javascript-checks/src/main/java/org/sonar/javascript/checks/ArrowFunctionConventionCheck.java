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
import org.sonar.check.RuleProperty;
import org.sonar.plugins.javascript.api.tree.SeparatedList;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.BindingElementTree;
import org.sonar.plugins.javascript.api.tree.declaration.ParameterListTree;
import org.sonar.plugins.javascript.api.tree.expression.ArrowFunctionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.tree.statement.ReturnStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.StatementTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;

@Rule(key = "S3524")
public class ArrowFunctionConventionCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE_ADD_PARAMETER = "在此箭头函数的参数周围添加括号。";
  private static final String MESSAGE_ADD_BODY = "将花括号和 \"return\" 添加到此箭头函数主体。";
  private static final String MESSAGE_REMOVE_PARAMETER = "删除此箭头函数的参数周围的括号。";
  private static final String MESSAGE_REMOVE_BODY = "从箭头函数主体中删除花括号和\"return\" 。";

  private static final boolean DEFAULT_PARAMETER_PARENS = false;
  private static final boolean DEFAULT_BODY_BRACES = false;

  @RuleProperty(
    key = "parameter_parens",
    description = "如果为True，则参数周围需要括号。False，则禁止它们用于单个参数。",
    defaultValue = "" + DEFAULT_PARAMETER_PARENS)
  private boolean parameterParens = DEFAULT_PARAMETER_PARENS;

  @RuleProperty(
    key = "body_braces",
    description = "如果为True，则要求函数主体周围使用花括号。False，则禁止它们返回单项机构。",
    defaultValue = "" + DEFAULT_BODY_BRACES)
  private boolean bodyBraces = DEFAULT_BODY_BRACES;

  public void setParameterParens(boolean value) {
    parameterParens = value;
  }
  public void setBodyBraces(boolean value) {
    bodyBraces = value;
  }

  @Override
  public void visitArrowFunction(ArrowFunctionTree tree) {
    checkParameterClause(tree);
    checkBody(tree);

    super.visitArrowFunction(tree);
  }

  private void checkBody(ArrowFunctionTree tree) {
    boolean hasBodyBraces = tree.body().is(Kind.BLOCK);

    if (bodyBraces && !hasBodyBraces) {
      addIssue(tree.body(), MESSAGE_ADD_BODY);
    }

    if (!bodyBraces && hasBodyBraces) {
      List<StatementTree> statements = ((BlockTree) tree.body()).statements();

      if (statements.size() == 1) {
        StatementTree statement = statements.get(0);

        if (isRemovableReturn(statement)) {
          addIssue(tree.body(), MESSAGE_REMOVE_BODY);
        }
      }
    }
  }

  private static boolean isRemovableReturn(StatementTree tree) {
    if (tree.is(Kind.RETURN_STATEMENT)) {
      ExpressionTree expression = ((ReturnStatementTree) tree).expression();

      if (expression != null && !expression.is(Kind.OBJECT_LITERAL)) {
        int firstLine = expression.firstToken().line();
        int lastLine = expression.lastToken().line();
        return firstLine == lastLine;
      }
    }

    return false;
  }

  private void checkParameterClause(ArrowFunctionTree tree) {
    boolean hasParameterParens = tree.parameterClause().is(Kind.PARAMETER_LIST);

    if (parameterParens && !hasParameterParens) {
      addIssue(tree.parameterClause(), MESSAGE_ADD_PARAMETER);
    }

    if (!parameterParens && hasParameterParens) {
      ParameterListTree parameterListTree = (ParameterListTree) tree.parameterClause();
      SeparatedList<BindingElementTree> parameters = parameterListTree.parameters();
      if (parameters.size() == 1 && parameters.get(0).is(Kind.BINDING_IDENTIFIER) && !hasCommentInside(parameterListTree)) {
        addIssue(tree.parameterClause(), MESSAGE_REMOVE_PARAMETER);
      }
    }
  }

  private static boolean hasCommentInside(ParameterListTree parameterListTree) {
    return !parameterListTree.closeParenthesisToken().trivias().isEmpty();
  }
}
