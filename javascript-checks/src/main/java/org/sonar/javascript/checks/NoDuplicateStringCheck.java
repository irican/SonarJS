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

@Rule(key = "S1192")
public class NoDuplicateStringCheck extends EslintBasedCheck {

  private static final int DEFAULT_THRESHOLD = 3;

  @RuleProperty(
    key = "threshold",
    description = "通过文字重复触发问题的次数",
    defaultValue = "" + DEFAULT_THRESHOLD)
  int threshold = DEFAULT_THRESHOLD;

  @Override
  public List<String> configurations() {
    return configurations(threshold);
  }

  @Override
  public String eslintKey() {
    return "no-duplicate-string";
  }
}
