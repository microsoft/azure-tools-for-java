/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveTableModel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.utils.Pair;
import org.apache.commons.lang3.StringUtils;

import javax.swing.event.TableModelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SubmissionTableModel extends InteractiveTableModel{
    private static final String[] columns = {"Key", "Value"};

    private List<SparkSubmissionJobConfigCheckResult> checkResults;

    public SubmissionTableModel() {
        super(columns);
    }

    public SubmissionTableModel(List<Pair<String, String>> flatJobConfig) {
        this();

        loadJobConfigMap(flatJobConfig);

        addTableModelListener(e -> {
            if ((e.getType() == TableModelEvent.UPDATE || e.getType() == TableModelEvent.INSERT)) {
                int updatedRow = e.getLastRow();
                String k = getValueAt(updatedRow, 0) == null ? null : getValueAt(updatedRow, 0).toString();
                String v = getValueAt(updatedRow, 1) == null ? null : getValueAt(updatedRow, 1).toString();

                if (StringUtils.isNotBlank(k) || StringUtils.isNotBlank(v)) {
                    if (updatedRow + 1 == getRowCount() && !hasEmptyRow()) {
                        addEmptyRow();
                    }
                }
            }
        });
    }

    @Nullable
    public SparkSubmissionJobConfigCheckResult getFirstCheckResults() {
        return checkResults == null || checkResults.size() == 0 ? null : checkResults.get(0);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0 ? !
                SparkSubmissionParameter.isSubmissionParameter((String) this.getValueAt(rowIndex, 0)) :
                super.isCellEditable(rowIndex, columnIndex);
    }

    @Override
    public void addRow(String key, Object value) {
        if (hasEmptyRow()) {
            int row = getRowCount() - 1;
            setValueAt(key, row, 0);
            setValueAt(value, row, 1);
        } else {
            super.addRow(key, value);
        }
    }

    @NotNull
    public List<Pair<String, String>> getJobConfigMap() {
        List<Pair<String, String>> jobConfigs = new ArrayList<>();

        for (int index = 0; index < this.getRowCount(); index++) {
            String key = (String) this.getValueAt(index, 0);
            String value = Optional.ofNullable(this.getValueAt(index, 1)).map(Object::toString).orElse(null);

            if (!(StringUtils.isBlank(key) && StringUtils.isBlank(value))) {
                jobConfigs.add(new Pair<>(key, value));
            }
        }

        checkResults = SparkSubmissionParameter.checkJobConfigMap(
                jobConfigs.stream().collect(Collectors.toMap(Pair::first, Pair::second)));

        return jobConfigs;
    }

    public void loadJobConfigMap(List<Pair<String, String>> jobConf) {
        // Not thread safe
        removeAllRows();

        jobConf.forEach(kvPair -> {
            if (!(StringUtils.isBlank(kvPair.first()) && StringUtils.isBlank(kvPair.second()))) {
                addRow(kvPair.first(), kvPair.second());
            }
        });

        if (!hasEmptyRow()) {
            addEmptyRow();
        }
    }
}
