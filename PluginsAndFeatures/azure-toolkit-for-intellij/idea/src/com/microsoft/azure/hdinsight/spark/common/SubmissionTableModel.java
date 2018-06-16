/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveTableModel;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

public class SubmissionTableModel extends InteractiveTableModel{
    private List<SparkSubmissionJobConfigCheckResult> checkResults;

    public SubmissionTableModel(String[] columnNames) {
        super(columnNames);
    }

    public SparkSubmissionJobConfigCheckResult getFirstCheckResults() {
        return checkResults == null || checkResults.size() == 0 ? null : checkResults.get(0);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        super.setValueAt(aValue, rowIndex, columnIndex);
        checkParameter();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0 ? !
                SparkSubmissionParameter.isSubmissionParameter((String) this.getValueAt(rowIndex, 0)) :
                super.isCellEditable(rowIndex, columnIndex);
    }

    @Override
    public void addRow(String key, Object value) {
        super.addRow(key, value);
        checkParameter();
    }

    @NotNull
    public Map<String, Object> getJobConfigMap() {
        Map<String, Object> jobConfigMap = new HashMap<>();
        Map<String, Object> sparkConfigMap = new HashMap<>();

        for (int index = 0; index < this.getRowCount(); index++) {
            String key = (String) this.getValueAt(index, 0);

            if (!StringHelper.isNullOrWhiteSpace(key)) {
                // Separate the submission and Spark conf parameters
                if (SparkSubmissionParameter.isSubmissionParameter(key)) {
                    jobConfigMap.put(key, this.getValueAt(index, 1));
                } else {
                    sparkConfigMap.put(key, this.getValueAt(index, 1));
                }
            }
        }

        if (!sparkConfigMap.isEmpty()) {
            jobConfigMap.put(SparkSubmissionParameter.Conf, sparkConfigMap);
        }

        return jobConfigMap;
    }

    public void loadJobConfigMap(Map<String, Object> jobConf) {
        removeAllRows();

        Stream.concat(
                jobConf.entrySet().stream()
                        .filter(entry -> SparkSubmissionParameter.isSubmissionParameter(entry.getKey())),
                // The Spark Job Configuration needs to be separated
                jobConf.entrySet().stream()
                        .filter(entry -> !SparkSubmissionParameter.isSubmissionParameter(entry.getKey()))
                        .filter(entry -> entry.getKey().equals(SparkSubmissionParameter.Conf))
                        .flatMap(entry -> new SparkConfigures(entry.getValue()).entrySet().stream())
        )
        .filter(entry -> !entry.getKey().trim().isEmpty())
        .forEach(entry -> super.addRow(entry.getKey(), entry.getValue()));

        if (!hasEmptyRow()) {
            addEmptyRow();
        }

        checkParameter();
    }

    public void loadJobConfigMapFromPropertyFile(String propertyFilePath) {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(propertyFilePath);

            prop.load(input);

            removeAllRows();

            Enumeration<?> e = prop.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String value = prop.getProperty(key);

                if (!StringUtils.isNullOrWhiteSpace(key)) {
                    super.addRow(key, value);
                }
            }

            if (!hasEmptyRow()) {
                addEmptyRow();
            }

            checkParameter();
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError("Fail to read Spark property file: " + propertyFilePath, ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    DefaultLoader.getUIHelper().logError("Fail to close Spark property file: " + propertyFilePath, ex);
                }
            }
        }
    }

    private void checkParameter() {
        final List<SparkSubmissionJobConfigCheckResult> resultList = SparkSubmissionParameter.checkJobConfigMap(getJobConfigMap());

        Collections.sort(resultList, new Comparator<SparkSubmissionJobConfigCheckResult>() {
            @Override
            public int compare(SparkSubmissionJobConfigCheckResult o1, SparkSubmissionJobConfigCheckResult o2) {
                if (o1.getStatus() == o2.getStatus()) {
                    return 0;
                } else if (o1.getStatus() == SparkSubmissionJobConfigCheckStatus.Warning && o2.getStatus() == SparkSubmissionJobConfigCheckStatus.Error) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        checkResults = resultList;
    }

}
