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

import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.JarArtifactType;
import com.intellij.platform.ProjectTemplate;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.projects.ui.HDInsightProjectTypeStep;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HDInsightModuleBuilder extends JavaModuleBuilder implements ModuleBuilderListener {
    private HDInsightProjectTemplate selectedTemplate;
    private HDInsightExternalSystem selectedExternalSystem;
    private List<ProjectTemplate> templates;
    private SparkVersion sparkVersion;
    private String sbtVersion;

    public HDInsightModuleBuilder() {
        this.selectedExternalSystem = HDInsightExternalSystem.MAVEN;
        initTemplates();
        this.addListener(this);
    }

    @Override
    public String getBuilderId() {
        return "HDInsight";
    }

    @Override
    public Icon getBigIcon() {
        return null;
    }

    @Override
    public Icon getNodeIcon() {
        return IconLoader.getIcon(CommonConst.ProductIConPath);
    }

    @Override
    public String getPresentableName() {
        return "HDInsight";
    }

    @Override
    public String getGroupName() {
        return "HDInsight Tools";
    }

    @Override
    public ModuleType getModuleType() {
        return HDInsightModuleType.getInstance();
    }

    @Override
    public ModuleWizardStep modifySettingsStep(SettingsStep settingsStep) {
        switch (this.selectedTemplate.getTemplateType()) {
            case Scala:
            case ScalaClusterSample:
            case ScalaLocalSample:
                return new SparkScalaSettingsStep(this, settingsStep);
            default:
                return new SparkJavaSettingsStep(this, settingsStep);
        }
    }

    @NotNull
    @Override
    public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        if (this.selectedExternalSystem == HDInsightExternalSystem.SBT) {
            // update module file name to lower case
            // some with the logic in scala plugin when create SBT module
            this.updateModulePath();
        }
        return super.createModule(moduleModel);
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        HDInsightProjectTypeStep step = new HDInsightProjectTypeStep(this);
        Disposer.register(parentDisposable, step);
        return step;
    }

    @Override
    public void moduleCreated(@NotNull Module module) {
        createDefaultArtifact(module);
        switch(this.selectedExternalSystem) {
            case MAVEN:
                new MavenProjectGenerator(module, this.selectedTemplate.getTemplateType(), sparkVersion).generate();
                break;
            case SBT:
                new SbtProjectGenerator(module, this.selectedTemplate.getTemplateType(), sparkVersion, sbtVersion).generate();
                break;
            default:
                new MavenProjectGenerator(module, this.selectedTemplate.getTemplateType(), sparkVersion).generate();
        }

        addTelemetry(this.selectedTemplate.getTemplateType(), sparkVersion);
    }

    public void setSparkVersion(SparkVersion sparkVersion) {
        this.sparkVersion = sparkVersion;
    }

    public void setSelectedTemplate(HDInsightProjectTemplate selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public void setSbtVersion(String sbtVersion) {
        this.sbtVersion = sbtVersion;
    }

    public void setSelectedExternalSystem(HDInsightExternalSystem selectedExternalSystem) {
        this.selectedExternalSystem = selectedExternalSystem;
    }

    public List<ProjectTemplate> getTemplates() {
        return this.templates;
    }

    public HDInsightExternalSystem getSelectedExternalSystem() {
        return this.selectedExternalSystem;
    }

    private void updateModulePath() {
        File file = new File(this.getModuleFilePath());
        String path = file.getParent() + "/" + file.getName().toLowerCase();
        this.setModuleFilePath(path);
    }

    private void initTemplates() {
        this.templates = new ArrayList<>();
        this.templates.add(new HDInsightProjectTemplate(HDInsightTemplatesType.Java));
        this.templates.add(new HDInsightProjectTemplate(HDInsightTemplatesType.Scala));
        this.templates.add(new HDInsightProjectTemplate(HDInsightTemplatesType.JavaLocalSample));
        this.templates.add(new HDInsightProjectTemplate(HDInsightTemplatesType.ScalaLocalSample));
        this.templates.add(new HDInsightProjectTemplate(HDInsightTemplatesType.ScalaClusterSample));
    }

    private void addTelemetry(HDInsightTemplatesType templatesType, SparkVersion sparkVersion) {
        Map<String, String> hdiProperties = new HashMap<String, String>();
        hdiProperties.put("Spark Version", sparkVersion.toString());
        hdiProperties.put("Build Tool", selectedExternalSystem.toString());

        if (templatesType == HDInsightTemplatesType.Java) {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemJavaCreation"), null, hdiProperties);
        } else if (templatesType == HDInsightTemplatesType.JavaLocalSample) {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemJavaSampleCreation"), null, hdiProperties);
        } else if (templatesType == HDInsightTemplatesType.Scala) {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemScalaCreation"), null, hdiProperties);
        } else if (templatesType == HDInsightTemplatesType.ScalaClusterSample || templatesType == HDInsightTemplatesType.ScalaLocalSample) {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemScalaSampleCreation"), null, hdiProperties);
        } else {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemOtherCreation"), null, hdiProperties);
        }
    }

    private void createDefaultArtifact(final Module module) {
        final Project project = module.getProject();
        final JarArtifactType type = new JarArtifactType();
        final PackagingElementFactory factory = PackagingElementFactory.getInstance();
        CompositePackagingElement root = factory.createArchive("default_artifact.jar");
        root.addOrFindChild(factory.createModuleOutput(module));
        ArtifactManager.getInstance(project).addArtifact(module.getName() + "_DefaultArtifact", type, root);
    }
}
