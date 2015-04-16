/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript.ast.resolve;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.source.Symbolizable;
import org.sonar.javascript.api.EcmaScriptPunctuator;
import org.sonar.javascript.ast.visitors.BaseTreeVisitor;
import org.sonar.javascript.highlighter.HighlightSymbolTableBuilder;
import org.sonar.javascript.highlighter.SourceFileOffsets;
import org.sonar.javascript.model.interfaces.Tree;
import org.sonar.javascript.model.interfaces.declaration.FunctionDeclarationTree;
import org.sonar.javascript.model.interfaces.declaration.MethodDeclarationTree;
import org.sonar.javascript.model.interfaces.declaration.ScriptTree;
import org.sonar.javascript.model.interfaces.expression.ArrowFunctionTree;
import org.sonar.javascript.model.interfaces.expression.AssignmentExpressionTree;
import org.sonar.javascript.model.interfaces.expression.FunctionExpressionTree;
import org.sonar.javascript.model.interfaces.expression.IdentifierTree;
import org.sonar.javascript.model.interfaces.expression.UnaryExpressionTree;
import org.sonar.javascript.model.interfaces.statement.CatchBlockTree;
import org.sonar.javascript.model.interfaces.statement.ForInStatementTree;
import org.sonar.javascript.model.interfaces.statement.ForOfStatementTree;

import javax.annotation.Nullable;

public class SymbolVisitor extends BaseTreeVisitor {

  private static final Logger LOG = LoggerFactory.getLogger(SymbolVisitor.class);

  private final Symbolizable symbolizable;
  private final SourceFileOffsets sourceFileOffsets;

  private SymbolModel symbolModel;
  private Scope currentScope;

  public SymbolVisitor(SymbolModel symbolModel, @Nullable Symbolizable symbolizable, @Nullable SourceFileOffsets sourceFileOffsets) {
    this.symbolModel = symbolModel;
    this.currentScope = null;

    // Symbol highlighting
    this.symbolizable = symbolizable;
    this.sourceFileOffsets = sourceFileOffsets;
  }

  @Override
  public void visitScript(ScriptTree tree) {
    // First pass to record symbol declarations
    new SymbolDeclarationVisitor(symbolModel).visitScript(tree);

    enterScope(tree);
    addBuildInSymbols();
    // Record usage and implicit symbol declarations
    super.visitScript(tree);
    leaveScope();

    highlightSymbols();
  }

  private void addBuildInSymbols() {
    createBuildInSymbolForScope("eval", currentScope.globalScope(), Symbol.Kind.FUNCTION);
  }

  @Override
  public void visitMethodDeclaration(MethodDeclarationTree tree) {
    enterScope(tree);
    super.visitMethodDeclaration(tree);
    leaveScope();
  }

  @Override
  public void visitCatchBlock(CatchBlockTree tree) {
    enterScope(tree);
    super.visitCatchBlock(tree);
    leaveScope();
  }

  @Override
  public void visitFunctionDeclaration(FunctionDeclarationTree tree) {
    enterScope(tree);
    super.visitFunctionDeclaration(tree);
    leaveScope();
  }

  @Override
  public void visitFunctionExpression(FunctionExpressionTree tree) {
    enterScope(tree);
    super.visitFunctionExpression(tree);
    leaveScope();
  }

  @Override
  public void visitArrowFunction(ArrowFunctionTree tree) {
    enterScope(tree);
    super.visitArrowFunction(tree);
    leaveScope();
  }

  /**
   * When an assignment is done to a symbol that has not been declared before,
   * a global variable is created with the left-hand side identifier as name.
   */
  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (tree.variable() instanceof IdentifierTree) {
      IdentifierTree identifier = (IdentifierTree) tree.variable();
      Usage.Kind usageKind = Usage.Kind.WRITE;
      if (!tree.operator().text().equals(EcmaScriptPunctuator.EQU.getValue())) {
        usageKind = Usage.Kind.READ_WRITE;
      }

      if (!addUsageFor(identifier, tree, usageKind)) {
        Symbol symbol = createSymbolForScope(identifier.name(), identifier, currentScope.globalScope(), Symbol.Kind.VARIABLE);
        Usage.createInit(symbolModel, symbol, identifier, tree, usageKind);
      }
      // no need to scan variable has it has been handle
      scan(tree.expression());

    } else {
      super.visitAssignmentExpression(tree);
    }
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER_REFERENCE)) {
      addUsageFor(tree, null, Usage.Kind.READ);
    }
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if (isIncDec(tree) && tree.expression().is(Tree.Kind.IDENTIFIER_REFERENCE)){
      addUsageFor((IdentifierTree)tree.expression(), null, Usage.Kind.READ_WRITE);
    } else {
      super.visitUnaryExpression(tree);
    }
  }

  private boolean isIncDec(UnaryExpressionTree tree) {
    return tree.is(
        Tree.Kind.PREFIX_INCREMENT,
        Tree.Kind.PREFIX_DECREMENT,
        Tree.Kind.POSTFIX_INCREMENT,
        Tree.Kind.POSTFIX_DECREMENT
    );
  }

  @Override
  public void visitForOfStatement(ForOfStatementTree tree) {
    if (tree.variableOrExpression() instanceof IdentifierTree) {
      IdentifierTree identifier = (IdentifierTree) tree.variableOrExpression();

      if (!addUsageFor(identifier, null, Usage.Kind.WRITE)) {
        createSymbolForScope(identifier.name(), identifier, currentScope.globalScope(), Symbol.Kind.VARIABLE);
      }
    }
    super.visitForOfStatement(tree);
  }

  @Override
  public void visitForInStatement(ForInStatementTree tree) {
    if (tree.variableOrExpression() instanceof IdentifierTree) {
      IdentifierTree identifier = (IdentifierTree) tree.variableOrExpression();

      if (!addUsageFor(identifier, null, Usage.Kind.WRITE)) {
        createSymbolForScope(identifier.name(), identifier, currentScope.globalScope(), Symbol.Kind.VARIABLE);
      }

      scan(tree.expression());
      scan(tree.statement());

    } else {
      super.visitForInStatement(tree);
    }
  }

  /*
   * HELPERS
   */
  private void leaveScope() {
    if (currentScope != null) {
      currentScope = currentScope.outer();
    }
  }

  private Symbol createSymbolForScope(String name, IdentifierTree tree, Scope scope, Symbol.Kind kind) {
    //todo(Lena): move this logic to method of Scope or SymbolModel
    Symbol symbol = scope.createSymbol(name, new SymbolDeclaration(tree, /*todo replace it with smth else?*/ SymbolDeclaration.Kind.FOR_OF), kind);
    symbolModel.setScopeForSymbol(symbol, scope);
    symbolModel.setScopeFor(tree, scope);
    return symbol;
  }

  private Symbol createBuildInSymbolForScope(String name, Scope scope, Symbol.Kind kind) {
    Symbol symbol = scope.createBuildInSymbol(name, kind);
    symbolModel.setScopeForSymbol(symbol, scope);
    symbolModel.setScopeFor(scope.getTree(), scope);
    return symbol;
  }

  private void enterScope(Tree tree) {
    currentScope = symbolModel.getScopeFor(tree);
  }

  /**
   * @return true if symbol found and usage recorded, false otherwise.
   */
  private boolean addUsageFor(IdentifierTree identifier, @Nullable Tree usageTree, Usage.Kind kind) {
    Symbol symbol = currentScope.lookupSymbol(identifier.name());
    if (symbol != null) {
      if (usageTree != null){
        Usage.create(symbolModel, symbol, identifier, usageTree, kind);
      } else {
        Usage.create(symbolModel, symbol, identifier, kind);
      }
      return true;
    }
    return false;
  }

  private void highlightSymbols() {
    if (symbolizable != null) {
      symbolizable.setSymbolTable(HighlightSymbolTableBuilder.build(symbolizable, symbolModel, sourceFileOffsets));
    } else {
      LOG.warn("Symbol in source view will not be highlighted.");
    }
  }

}
