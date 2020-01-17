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
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.javascript.checks.utils.CheckUtils;
import org.sonar.javascript.tree.KindSet;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.Usage;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.BindingElementTree;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionTree;
import org.sonar.plugins.javascript.api.tree.declaration.ParameterListTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.statement.CatchBlockTree;
import org.sonar.plugins.javascript.api.tree.statement.ForObjectStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.VariableDeclarationTree;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;

@Rule(key = "S1226")
public class ReassignedParameterCheck extends SubscriptionVisitorCheck {

  private static final String MESSAGE = "引入一个新变量，而不是重新使用 %s \"%s\".";

  private static final String FOREACH_VARIABLE = "foreach变量";
  private static final String PARAMETER = "参数";
  private static final String CAUGHT_EXCEPTION = "捕获的异常";

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.CATCH_BLOCK)) {
      checkBindingElement((BindingElementTree) ((CatchBlockTree) tree).parameter(), CAUGHT_EXCEPTION);

    } else if (tree.is(Kind.FOR_IN_STATEMENT, Kind.FOR_OF_STATEMENT)) {
      checkForLoop((ForObjectStatementTree) tree);

    } else {
      checkFunctionParameters((FunctionTree) tree);
    }
  }

  private void checkFunctionParameters(FunctionTree functionTree) {
    Tree parameterClause = functionTree.parameterClause();

    if (parameterClause.is(Kind.BINDING_IDENTIFIER)) {
      checkBindingElement((IdentifierTree) parameterClause, PARAMETER);

    } else {
      for (Tree parameterTree : ((ParameterListTree) parameterClause).parameters()) {
        checkBindingElement((BindingElementTree) parameterTree, PARAMETER);
      }
    }
  }

  private void checkForLoop(ForObjectStatementTree forLoop) {
    Tree variableOrExpression = forLoop.variableOrExpression();

    if (variableOrExpression instanceof VariableDeclarationTree) {
      for (BindingElementTree bindingElementTree : ((VariableDeclarationTree) variableOrExpression).variables()) {
        checkBindingElement(bindingElementTree, FOREACH_VARIABLE);
      }

    } else if (variableOrExpression.is(Kind.IDENTIFIER_REFERENCE)) {
      IdentifierTree identifier = (IdentifierTree) variableOrExpression;
      identifier.symbol().ifPresent(s -> checkSymbol(s, identifier, forLoop, FOREACH_VARIABLE));
    }
  }

  private void checkBindingElement(BindingElementTree parameter, String title) {
    for (IdentifierTree identifierTree : parameter.bindingIdentifiers()) {
      // symbol for identifier from binding element should never be null
      identifierTree.symbol().ifPresent(s -> checkSymbol(s, identifierTree, null, title));
    }
  }

  private void checkSymbol(Symbol symbol, IdentifierTree declarationIdentifier, @Nullable ForObjectStatementTree loop, String title) {

    for (Usage usage : symbol.usages()) {

      if (usage.isWrite() &&
        !usage.identifierTree().equals(declarationIdentifier)
        && (loop == null || CheckUtils.isDescendant(usage.identifierTree(), loop))) {

        addIssue(usage.identifierTree(), String.format(MESSAGE, title, symbol.name()));
      }
    }
  }

  @Override
  public Set<Kind> nodesToVisit() {
    return ImmutableSet.<Kind>builder()
      .addAll(KindSet.FUNCTION_KINDS.getSubKinds())
      .add(Kind.CATCH_BLOCK)
      .add(Kind.FOR_IN_STATEMENT)
      .add(Kind.FOR_OF_STATEMENT)
      .build();
  }
}
