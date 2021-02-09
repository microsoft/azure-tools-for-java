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

public class SparkScalaSettingsStep extends ModuleWizardStep {
    private HDInsightModuleBuilder builder;
    private ModuleWizardStep javaStep;
    private SparkVersionOptionsPanel sparkVersionOptionsPanel;
    private SbtVersionOptionsPanel sbtVersionOptionsPanel;

    public SparkScalaSettingsStep(HDInsightModuleBuilder builder, SettingsStep settingsStep) {
        this.builder = builder;
        this.javaStep = StdModuleTypes.JAVA.modifyProjectTypeStep(settingsStep, builder);

        if (builder.getSelectedTemplate() != null &&
                builder.getSelectedTemplate().getTemplateType() == HDInsightTemplatesType.ScalaFailureTaskDebugSample) {
            this.sparkVersionOptionsPanel = new SparkVersionOptionsPanel(new SparkVersion[] {
                    SparkVersion.SPARK_2_1_0,
                    SparkVersion.SPARK_2_3_0,
                    SparkVersion.SPARK_2_3_2
            });
        } else {
            this.sparkVersionOptionsPanel = new SparkVersionOptionsPanel();
        }
        settingsStep.addSettingsField("Spark \u001Bversion:", this.sparkVersionOptionsPanel);

        if (builder.getSelectedExternalSystem() == HDInsightExternalSystem.SBT) {
            this.sbtVersionOptionsPanel = new SbtVersionOptionsPanel();
            settingsStep.addSettingsField("Sbt versio\u001Bn:", this.sbtVersionOptionsPanel);
            this.sbtVersionOptionsPanel.updateSbtVersions();
        }
    }

    @Override
    public JComponent getComponent() {
        return javaStep.getComponent();
    }

    @Override
    public void updateDataModel() {
        this.builder.setSparkVersion(this.sparkVersionOptionsPanel.apply());
        if (this.sbtVersionOptionsPanel != null) {
            this.builder.setSbtVersion(this.sbtVersionOptionsPanel.apply());
        }

        javaStep.updateDataModel();
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
