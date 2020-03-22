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
