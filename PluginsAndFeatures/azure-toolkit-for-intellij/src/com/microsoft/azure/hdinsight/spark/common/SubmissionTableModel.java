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
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
    }

    @Nullable
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
    public List<Pair<String, String>> getJobConfigMap() {
        List<Pair<String, String>> jobConfigs = new ArrayList<>();

        for (int index = 0; index < this.getRowCount(); index++) {
            String key = (String) this.getValueAt(index, 0);
            Object value = this.getValueAt(index, 1);

            if (!StringHelper.isNullOrWhiteSpace(key)) {
                jobConfigs.add(new Pair<>(key, value == null ? null : value.toString()));
            }
        }

        return jobConfigs;
    }

    public void loadJobConfigMap(List<Pair<String, String>> jobConf) {
        // Not thread safe
        removeAllRows();

        jobConf.forEach(kvPair -> {
            if (kvPair.first() != null) {
                super.addRow(kvPair.first(), kvPair.second());
            }
        });

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
        checkResults = SparkSubmissionParameter.checkJobConfigMap(
                getJobConfigMap().stream().collect(Collectors.toMap(Pair::first, Pair::second)));
    }

}
