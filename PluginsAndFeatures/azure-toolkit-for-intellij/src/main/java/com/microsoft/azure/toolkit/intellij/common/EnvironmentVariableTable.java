/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common;

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

    public void setEnv(Map<String, String> environmentVariables) {
        final List<EnvironmentVariable> environmentVariableList =
                environmentVariables.keySet().stream()
                                    .map(key -> new EnvironmentVariable(key, environmentVariables.get(key), false))
                                    .collect(Collectors.toList());
        setValues(environmentVariableList);
    }

    public Map<String, String> getEnv() {
        Map<String, String> result = new LinkedHashMap<>();
        for (EnvironmentVariable variable : this.getEnvironmentVariables()) {
            if (StringUtil.isEmpty(variable.getName())) {
                continue;
            }
            result.put(variable.getName(), variable.getValue());
        }
        return result;
    }
}
