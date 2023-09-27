/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.execution.ui.CommonProgramParametersPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.DocumentListener;
import java.lang.reflect.Field;
import java.util.Map;

public class SparkLocalRunCommonParametersPanel extends CommonJavaParametersPanel{

    public SparkLocalRunCommonParametersPanel() {
        super();
    }

    public void addWorkingDirectoryUpdateListener(DocumentListener listener) {
        myWorkingDirectoryField.getTextField().getDocument().addDocumentListener(listener);
    }

    @NotNull
    public String getWorkingDirectory() {
        return myWorkingDirectoryField.getText();
    }

    @NotNull
    public Map<String, String> getEnvs() {
        try {
            Field myEnvVariablesComponentField = CommonProgramParametersPanel.class.getDeclaredField("myEnvVariablesComponent");
            myEnvVariablesComponentField.setAccessible(true);
            return ((EnvironmentVariablesComponent) myEnvVariablesComponentField.get(this)).getEnvs();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
