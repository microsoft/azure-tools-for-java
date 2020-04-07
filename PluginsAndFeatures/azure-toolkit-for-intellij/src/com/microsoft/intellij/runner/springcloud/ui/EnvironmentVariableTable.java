/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.runner.springcloud.ui;

import com.intellij.execution.util.EnvVariablesTable;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.table.TableView;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnvironmentVariableTable extends EnvVariablesTable {

    public EnvironmentVariableTable() {
        TableView<EnvironmentVariable> tableView = getTableView();
        tableView.setPreferredScrollableViewportSize(
                new Dimension(tableView.getPreferredScrollableViewportSize().width,
                        tableView.getRowHeight() * JBTable.PREFERRED_SCROLLABLE_VIEWPORT_HEIGHT_IN_ROWS));
        setPasteActionEnabled(true);
    }

    public void setEnvironmentVariablesMap(Map<String, String> environmentVariablesMap) {
        final List<EnvironmentVariable> environmentVariables =
                environmentVariablesMap.keySet().stream()
                        .map(key -> new EnvironmentVariable(key, environmentVariablesMap.get(key), true))
                        .collect(Collectors.toList());
        setValues(environmentVariables);
    }

    public Map<String, String> getEnvironmentVariablesMap() {
        Map<String, String> result = new LinkedHashMap<>();
        for (EnvironmentVariable variable : this.getEnvironmentVariables()) {
            if (StringUtil.isEmpty(variable.getName()) && StringUtil.isEmpty(variable.getValue())) {
                continue;
            }
            result.put(variable.getName(), variable.getValue());
        }
        return result;
    }
}
