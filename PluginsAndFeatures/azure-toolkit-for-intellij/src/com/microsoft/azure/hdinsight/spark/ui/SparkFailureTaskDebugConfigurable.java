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

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.MacroAwareTextBrowseFolderListener;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.spark.run.SparkFailureTaskDebugSettingsModel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

public class SparkFailureTaskDebugConfigurable implements SettableControl<SparkFailureTaskDebugSettingsModel> {
    private TextFieldWithBrowseButton myFailureJobContextPathField;
    private JPanel myWholePanel;
    private JTextArea myLog4jPropertiesField;

    public SparkFailureTaskDebugConfigurable(Project myProject) {
        // Bind the folder file chooser for Failure Task Context file
        FileChooserDescriptor dataRootDirectoryChooser = FileChooserDescriptorFactory.createSingleFileDescriptor(
                CommonConst.SPARK_FAILURE_TASK_CONTEXT_EXTENSION);
        myFailureJobContextPathField.addBrowseFolderListener(
                new MacroAwareTextBrowseFolderListener(dataRootDirectoryChooser, myProject));

        myFailureJobContextPathField.getTextField().setName("myFailureJobContextPathFieldText");
        myFailureJobContextPathField.getButton().setName("myFailureJobContextPathFieldButton");
    }

    // Data --> Component
    @Override
    public void setData(@NotNull SparkFailureTaskDebugSettingsModel data) {
        myFailureJobContextPathField.setText(data.getFailureContextPath());
        if (StringUtils.isNotBlank(data.getLog4jProperties())) {
            myLog4jPropertiesField.setText(data.getLog4jProperties());
        }
    }

    // Component -> Data
    @Override
    public void getData(@NotNull SparkFailureTaskDebugSettingsModel data) {
        data.setFailureContextPath(myFailureJobContextPathField.getText());
        data.setLog4jProperties(myLog4jPropertiesField.getText());
    }

    @NotNull
    public JComponent getComponent() {
        return myWholePanel;
    }
}

