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
package org.sonar.javascript.metrics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.Version;
import org.sonar.javascript.tree.KindSet;
import org.sonar.javascript.visitors.JavaScriptFileImpl;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitor;
import org.sonar.plugins.javascript.api.visitors.TreeVisitorContext;

public class MetricsVisitor extends SubscriptionVisitor {

  private static final Kind[] CLASS_NODES = {
    Kind.CLASS_DECLARATION,
    Kind.CLASS_EXPRESSION
  };

  private final SensorContext sensorContext;
  private InputFile inputFile;
  private final Boolean ignoreHeaderComments;
  private FileLinesContextFactory fileLinesContextFactory;
  private Map<InputFile, Set<Integer>> projectExecutableLines;

  public MetricsVisitor(SensorContext context, Boolean ignoreHeaderComments, FileLinesContextFactory fileLinesContextFactory) {
    this.sensorContext = context;
    this.ignoreHeaderComments = ignoreHeaderComments;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.projectExecutableLines = new HashMap<>();
  }

  /**
   * Returns executable lines of code for files in project
   */
  public Map<InputFile, Set<Integer>> executableLines() {
    return projectExecutableLines;
  }

  @Override
  public Set<Kind> nodesToVisit() {
    Set<Kind> result = EnumSet.copyOf(KindSet.FUNCTION_KINDS.getSubKinds());
    result.addAll(Arrays.asList(CLASS_NODES));
    return result;
  }

  @Override
  public void leaveFile(Tree scriptTree) {
    saveComplexityMetrics(getContext());
    saveCounterMetrics(getContext());
    saveLineMetrics(getContext());
  }

  @Override
  public void visitFile(Tree scriptTree) {
    this.inputFile = ((JavaScriptFileImpl) getContext().getJavaScriptFile()).inputFile();
  }

  private void saveCounterMetrics(TreeVisitorContext context) {
    CounterVisitor counter = new CounterVisitor(context.getTopTree());
    saveMetric(CoreMetrics.FUNCTIONS, counter.getFunctionNumber());
    saveMetric(CoreMetrics.STATEMENTS, counter.getStatementsNumber());
    saveMetric(CoreMetrics.CLASSES, counter.getClassNumber());
  }

  private void saveComplexityMetrics(TreeVisitorContext context) {
    int fileComplexity = new ComplexityVisitor(true).getComplexity(context.getTopTree());

    saveMetric(CoreMetrics.COMPLEXITY, fileComplexity);

    if (sensorContext.getSonarQubeVersion().isGreaterThanOrEqual(Version.create(6,3))) {
      int cognitiveComplexity = new CognitiveComplexity().calculateScriptComplexity(context.getTopTree()).complexity();
      saveMetric(CoreMetrics.COGNITIVE_COMPLEXITY, cognitiveComplexity);
    }
  }

  private void saveLineMetrics(TreeVisitorContext context) {
    LineVisitor lineVisitor = new LineVisitor(context.getTopTree());
    Set<Integer> linesOfCode = lineVisitor.getLinesOfCode();

    saveMetric(CoreMetrics.NCLOC, lineVisitor.getLinesOfCodeNumber());

    CommentLineVisitor commentVisitor = new CommentLineVisitor(context.getTopTree(), ignoreHeaderComments);

    saveMetric(CoreMetrics.COMMENT_LINES, commentVisitor.getCommentLineNumber());

    FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(this.inputFile);

    linesOfCode.forEach(line -> fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1));

    Set<Integer> executableLines = new ExecutableLineVisitor(context.getTopTree()).getExecutableLines();
    projectExecutableLines.put(inputFile, executableLines);

    executableLines.forEach(line -> fileLinesContext.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1));
    fileLinesContext.save();
  }

  private <T extends Serializable> void saveMetric(Metric metric, T value) {
    sensorContext.<T>newMeasure()
      .withValue(value)
      .forMetric(metric)
      .on(inputFile)
      .save();
  }

  public static Kind[] getClassNodes() {
    return CLASS_NODES;
  }

}
