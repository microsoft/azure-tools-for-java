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
package com.microsoft.azure.hdinsight.projects.template;

import com.intellij.facet.impl.ui.libraries.LibraryOptionsPanel;
import com.intellij.framework.library.FrameworkLibraryVersionFilter;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.microsoft.azure.hdinsight.projects.HDInsightModuleBuilder;
import com.microsoft.azure.hdinsight.projects.SparkLibraryDescription;
import com.microsoft.azure.hdinsight.projects.SparkLibraryOptionsPanel;
import org.jetbrains.plugins.scala.project.template.ScalaLibraryDescription$;

import javax.swing.*;

public class CustomModuleWizardSetup extends ModuleWizardStep {
    private HDInsightModuleBuilder builder;
    private ModuleWizardStep javaStep;

    private CustomTemplateInfo info;
    private LibraryOptionsPanel scalaLibraryPanel;
    private SparkLibraryOptionsPanel sparkLibraryOptionsPanel;

    private boolean isNeedSparkSDK;
    private boolean isNeedScalaSDK;

    public CustomModuleWizardSetup(HDInsightModuleBuilder builder, SettingsStep settingsStep, LibrariesContainer librariesContainer, CustomTemplateInfo info) {
        this.builder = builder;
        this.javaStep = StdModuleTypes.JAVA.modifyProjectTypeStep(settingsStep, builder);
        this.info = info;
        this.isNeedSparkSDK = this.info.isNeedSparkSDK();
        this.isNeedScalaSDK = this.info.isNeedScalaSDK();

        if (isNeedScalaSDK) {
            this.scalaLibraryPanel = new LibraryOptionsPanel(ScalaLibraryDescription$.MODULE$, "",
                    FrameworkLibraryVersionFilter.ALL, librariesContainer, false);
            ((JButton) scalaLibraryPanel.getSimplePanel().getComponent(1)).setText("Select...");
            settingsStep.addSettingsField("Scala S\u001BDK:", scalaLibraryPanel.getSimplePanel());
        }

        if (isNeedSparkSDK) {
            sparkLibraryOptionsPanel = new SparkLibraryOptionsPanel(settingsStep.getContext().getProject(), librariesContainer, new SparkLibraryDescription());
            settingsStep.addSettingsField("Spark S\u001BDK:", sparkLibraryOptionsPanel);
        }
    }

    @Override
    public void updateDataModel() {
        if (isNeedScalaSDK) {
            this.builder.setScalaLibraryCompositionSettings(scalaLibraryPanel.apply());
        }

        if (isNeedSparkSDK) {
            this.builder.setSparkCompositionSettings(sparkLibraryOptionsPanel.apply());
        }

        javaStep.updateDataModel();
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        javaStep.disposeUIResources();
    }

    @Override
    public JComponent getComponent() {
        return javaStep.getComponent();
    }
}
