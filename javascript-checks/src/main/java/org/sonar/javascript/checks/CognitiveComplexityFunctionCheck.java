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
import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.javascript.metrics.CognitiveComplexity;
import org.sonar.javascript.metrics.FunctionDefiningModuleVisitor;
import org.sonar.javascript.tree.KindSet;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionTree;
import org.sonar.plugins.javascript.api.tree.expression.ArrowFunctionTree;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.visitors.PreciseIssue;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;

import static org.sonar.plugins.javascript.api.tree.Tree.Kind.ARROW_FUNCTION;
import static org.sonar.plugins.javascript.api.tree.Tree.Kind.SCRIPT;

@Rule(key = "S3776")
public class CognitiveComplexityFunctionCheck extends SubscriptionVisitorCheck {

  private static final String MESSAGE = "重构此功能以将其认知复杂度从%s降低到可以允许的%s。";
  private static final int DEFAULT_THRESHOLD = 15;

  @RuleProperty(
    key = "threshold",
    description = "The maximum authorized complexity.",
	             //最大授权的复杂度。
    defaultValue = "" + DEFAULT_THRESHOLD)
  private int threshold = DEFAULT_THRESHOLD;

  /* When complexity of nesting function is considered for this check, we ignore all nested in it functions */
  private Set<Tree> ignoredNestedFunctions = new HashSet<>();

  private Set<FunctionTree> functionsDefiningModule = new HashSet<>();

  @Override
  public Set<Kind> nodesToVisit() {
    return ImmutableSet.<Kind>builder()
      .addAll(KindSet.FUNCTION_KINDS.getSubKinds())
      .add(Kind.SCRIPT)
      .build();
  }

  @Override
  public void visitFile(Tree scriptTree) {
    ignoredNestedFunctions.clear();
    super.visitFile(scriptTree);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(SCRIPT)) {
      functionsDefiningModule = FunctionDefiningModuleVisitor.getFunctionsDefiningModule((ScriptTree) tree);
      return;
    }

    if (!ignoredNestedFunctions.contains(tree)) {
      CognitiveComplexity.ComplexityData complexityData = new CognitiveComplexity().calculateFunctionComplexity((FunctionTree) tree, functionsDefiningModule.contains(tree));
      ignoredNestedFunctions.addAll(complexityData.aggregatedNestedFunctions());

      if (complexityData.complexity() > threshold) {
        raiseIssue(complexityData, tree);
      }
    }
  }

  private void raiseIssue(CognitiveComplexity.ComplexityData complexityData, Tree function) {
    String message = String.format(MESSAGE, complexityData.complexity(), threshold);

    SyntaxToken primaryLocation = function.firstToken();
    if (function.is(ARROW_FUNCTION)) {
      primaryLocation = ((ArrowFunctionTree) function).doubleArrowToken();
    }

    PreciseIssue issue = addIssue(primaryLocation, message).cost(complexityData.complexity() - (double)threshold);

    complexityData.secondaryLocations().forEach(issue::secondary);
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

}
