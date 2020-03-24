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

package com.microsoft.azure.hdinsight.projects;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.HDINSIGHT;

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
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.JarArtifactType;
import com.intellij.platform.ProjectTemplate;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.IconPathBuilder;
import com.microsoft.azure.hdinsight.projects.ui.HDInsightProjectTypeStep;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class HDInsightModuleBuilder extends JavaModuleBuilder implements ModuleBuilderListener {
    private HDInsightProjectTemplate selectedTemplate;
    private HDInsightExternalSystem selectedExternalSystem;
    private List<ProjectTemplate> templates;
    private SparkVersion sparkVersion;
    private String sbtVersion;
    private PackagingElementFactory artifactPackagingFactory;

    public HDInsightModuleBuilder() {
        this.selectedExternalSystem = HDInsightExternalSystem.MAVEN;
        initTemplates();
        this.addListener(this);
    }

    @Override
    public String getBuilderId() {
        return "HDInsight";
    }

    public Icon getBigIcon() {
        return null;
    }

    @Override
    public Icon getNodeIcon() {
        return IconLoader.getIcon(IconPathBuilder
                .custom(CommonConst.ProductIconName)
                .build());
    }

    @Override
    public String getPresentableName() {
        return "Apache Spark/HDInsight";
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
            case ScalaFailureTaskDebugSample:
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
        Artifact artifact = createDefaultArtifact(module);
        switch(this.selectedExternalSystem) {
            case MAVEN:
                new MavenProjectGenerator(module, this.selectedTemplate.getTemplateType(), sparkVersion)
                        .generate()
                        .done(mavenProject -> {
                            if (getSelectedTemplate() != null && artifactPackagingFactory != null &&
                                    getSelectedTemplate().getTemplateType() == HDInsightTemplatesType.ScalaFailureTaskDebugSample) {

                                // TODO: Remove hardcoded packaging here with spark-tools being independent.
                                File sparkToolsJar = Paths.get(Objects.requireNonNull(module.getProject().getBasePath()),
                                                               "lib",
                                                               SparkToolsLib.INSTANCE.getJarFileName(this.sparkVersion)).toFile();

                                artifact.getRootElement().addOrFindChild(
                                        artifactPackagingFactory.createExtractedDirectoryWithParentDirectories(
                                                sparkToolsJar.getPath(), "/", "/"));
                            }

                        });
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

    @Nullable
    HDInsightProjectTemplate getSelectedTemplate() {
        return selectedTemplate;
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
        this.templates.add(new HDInsightProjectTemplate(HDInsightTemplatesType.ScalaClusterSample));
        this.templates.add(new HDInsightProjectTemplate(HDInsightTemplatesType.ScalaFailureTaskDebugSample));
    }

    private void addTelemetry(HDInsightTemplatesType templatesType, SparkVersion sparkVersion) {
        Map<String, String> hdiProperties = new HashMap<String, String>();
        hdiProperties.put("Spark Version", sparkVersion.toString());
        hdiProperties.put("Build Tool", selectedExternalSystem.toString());

        if (templatesType == HDInsightTemplatesType.Java) {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemJavaCreation"), null, hdiProperties);
            EventUtil.logEvent(EventType.info, HDINSIGHT, HDInsightBundle.message("SparkProjectSystemJavaCreation"),
                hdiProperties, null);
        } else if (templatesType == HDInsightTemplatesType.Scala) {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemScalaCreation"), null, hdiProperties);
            EventUtil.logEvent(EventType.info, HDINSIGHT, HDInsightBundle.message("SparkProjectSystemScalaCreation"),
                hdiProperties, null);
        } else if (templatesType == HDInsightTemplatesType.ScalaClusterSample) {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemScalaSampleCreation"), null, hdiProperties);
            EventUtil.logEvent(EventType.info, HDINSIGHT, HDInsightBundle.message("SparkProjectSystemScalaSampleCreation"),
                    hdiProperties, null);
        } else if (templatesType == HDInsightTemplatesType.ScalaFailureTaskDebugSample) {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemScalaFailureTaskDebugSampleCreation"), null, hdiProperties);
            EventUtil.logEvent(EventType.info, HDINSIGHT,
                HDInsightBundle.message("SparkProjectSystemScalaFailureTaskDebugSampleCreation"), hdiProperties, null);
        } else {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemOtherCreation"), null, hdiProperties);
            EventUtil.logEvent(EventType.info, HDINSIGHT,
                HDInsightBundle.message("SparkProjectSystemOtherCreation"), hdiProperties, null);
        }
    }

    private Artifact createDefaultArtifact(final Module module) {
        final Project project = module.getProject();
        final JarArtifactType type = new JarArtifactType();
        artifactPackagingFactory = PackagingElementFactory.getInstance();

        final CompositePackagingElement root = artifactPackagingFactory.createArchive("default_artifact.jar");
        root.addOrFindChild(artifactPackagingFactory.createModuleOutput(module));

        return ArtifactManager.getInstance(project).addArtifact(module.getName() + "_DefaultArtifact", type, root);
    }
}
