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
