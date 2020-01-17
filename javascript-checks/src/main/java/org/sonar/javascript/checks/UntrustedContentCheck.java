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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.LiteralTree;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;

@Rule(key = "S2611")
public class UntrustedContentCheck extends SubscriptionVisitorCheck {

  private static final String MESSAGE = "从不受信任的来源中删除此内容。";

  @RuleProperty(
    key = "domainsToIgnore",
    description = "可忽略以逗号分隔的域列表。可以使用正则表达式，例如 (.*\\.)?example\\.com,foo\\.org"
  )
  public String domainsToIgnore = "";
  private List<Pattern> patterns = null;

  @Override
  public Set<Tree.Kind> nodesToVisit() {
    return ImmutableSet.of(Tree.Kind.STRING_LITERAL);
  }

  @Override
  public void visitFile(Tree tree) {

    patterns = new ArrayList<>();
    String[] pieces = domainsToIgnore.split(",");
    for (String piece : pieces) {
      patterns.add(Pattern.compile(piece));
    }
  }

  @Override
  public void visitNode(Tree tree) {

    LiteralTree literal = (LiteralTree) tree;

    String value = literal.value();
    value = value.substring(1, value.length() - 1);
    if (value.matches("^http.*")) {
      try {
        URI uri = new URI(value);
        if (isBad(uri)) {
          addIssue(tree, MESSAGE);
        }
      } catch (URISyntaxException e) {
        // we don't consider uris which could not be parsed
      }
    }
  }

  private boolean isBad(URI uri) {

    for (Pattern pattern : patterns) {
      String host = uri.getHost();
      if (host == null || pattern.matcher(host).matches()) {
        return false;
      }
    }
    return true;
  }
}
