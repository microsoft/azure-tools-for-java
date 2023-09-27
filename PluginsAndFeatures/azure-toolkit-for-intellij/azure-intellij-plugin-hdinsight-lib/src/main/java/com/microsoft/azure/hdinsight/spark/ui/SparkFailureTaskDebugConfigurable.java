/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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

        myFailureJobContextPathField.getTextField().setName("failureJobContextPathFieldText");
        myFailureJobContextPathField.getButton().setName("failureJobContextPathFieldButton");
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

