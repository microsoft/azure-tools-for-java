/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.console;

import com.intellij.openapi.options.SettingsEditor;

import javax.swing.*;

public class SparkScalaLivyConsoleRunConfigurationEditor extends SettingsEditor<SparkScalaLivyConsoleRunConfiguration> {
    private JPanel mainPanel;

    @Override
    protected void resetEditorFrom(final SparkScalaLivyConsoleRunConfiguration srcConf) {
    }

    @Override
    protected void applyEditorTo(final SparkScalaLivyConsoleRunConfiguration destConf) {
    }

    @Override
    protected JComponent createEditor() {
        return mainPanel;
    }
}
