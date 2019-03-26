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
                    SparkVersion.SPARK_2_3_0
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
