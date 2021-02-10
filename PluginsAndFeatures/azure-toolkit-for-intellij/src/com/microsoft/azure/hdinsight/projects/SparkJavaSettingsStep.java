/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;

import javax.swing.*;

public class SparkJavaSettingsStep extends ModuleWizardStep {
    private HDInsightModuleBuilder builder;
    private ModuleWizardStep javaStep;
    private SparkVersionOptionsPanel sparkVersionOptionsPanel;

    public SparkJavaSettingsStep(HDInsightModuleBuilder builder, SettingsStep settingsStep) {
        this.builder = builder;
        this.javaStep = StdModuleTypes.JAVA.modifyProjectTypeStep(settingsStep, builder);

        this.sparkVersionOptionsPanel = new SparkVersionOptionsPanel();
        settingsStep.addSettingsField("Spark \u001Bversion:", sparkVersionOptionsPanel);
    }

    @Override
    public JComponent getComponent() {
        return javaStep.getComponent();
    }

    @Override
    public void updateDataModel() {
        javaStep.updateDataModel();
        this.builder.setSparkVersion(this.sparkVersionOptionsPanel.apply());
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return super.validate() && (javaStep == null || javaStep.validate());
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        javaStep.disposeUIResources();
    }
}
