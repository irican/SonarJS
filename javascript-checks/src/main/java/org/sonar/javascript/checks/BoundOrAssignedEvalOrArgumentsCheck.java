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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.Usage;
import org.sonar.plugins.javascript.api.tree.ScriptTree;

@Rule(key = "BoundOrAssignedEvalOrArguments")
public class BoundOrAssignedEvalOrArgumentsCheck extends AbstractSymbolNameCheck {


  private static final String DECLARATION_MESSAGE = "请勿使用 \"%s\" 声明 %s - 使用其他名称。";
  private static final String MODIFICATION_MESSAGE = "删除对 \"%s\" 的修改。";

  @Override
  List<String> illegalNames() {
    return ImmutableList.of("eval", "arguments");
  }

  @Override
  public void visitScript(ScriptTree tree) {
    for (Symbol symbol : getIllegalSymbols()) {
      if (symbol.is(Symbol.Kind.PARAMETER) || !symbol.external()) {
        raiseIssuesOnDeclarations(symbol, String.format(DECLARATION_MESSAGE, symbol.name(), symbol.kind().getValue()));
      } else {
        raiseIssuesOnWriteUsages(symbol);
      }
    }
  }

  private void raiseIssuesOnWriteUsages(Symbol symbol) {
    for (Usage usage : symbol.usages()) {
      if (!usage.kind().equals(Usage.Kind.READ)) {
        addIssue(usage.identifierTree(), String.format(MODIFICATION_MESSAGE, symbol.name()));
      }
    }
  }

}
