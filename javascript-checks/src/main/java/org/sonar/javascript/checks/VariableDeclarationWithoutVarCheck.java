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
import org.sonar.check.Rule;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.Usage;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;

@Rule(key = "S2703")
public class VariableDeclarationWithoutVarCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "将\"let\", \"const\" 或 \"var\" 关键字添加到 \"%s\" 的声明中，以使这个声明为显式声明。";

  private static final Set<String> EXCLUDED_NAMES = ImmutableSet.of("exports", "module");

  @Override
  public void visitScript(ScriptTree tree) {
    for (Symbol symbol : getContext().getSymbolModel().getSymbols()) {
      if (symbol.isVariable() && !symbol.external() && !EXCLUDED_NAMES.contains(symbol.name())) {
        visitSymbol(symbol);
      }
    }
  }

  private void visitSymbol(Symbol symbol) {
    for (Usage usage : symbol.usages()) {
      if (usage.isDeclaration()) {
        return;
      }
    }
    if (!symbol.usages().isEmpty()) {
      addIssue(symbol.usages().iterator().next().identifierTree(), String.format(MESSAGE, symbol.name()));
    }
  }
}
