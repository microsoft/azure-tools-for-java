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
package com.microsoft.azure.hdinsight.spark.run.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.search.GlobalSearchScope;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobSubmissionState;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel;
import com.microsoft.azuretools.utils.Pair;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class RemoteDebugRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule> {
    private static final String SUBMISSION_CONTENT_NAME = "spark_submission";
    private static final String SUBMISSION_ATTRIBUTE_CLUSTER_NAME = "cluster_name";
    private static final String SUBMISSION_ATTRIBUTE_SELECTED_CLUSTER = "selected_cluster";
    private static final String SUBMISSION_ATTRIBUTE_IS_LOCAL_ARTIFACT = "is_local_artifact";
    private static final String SUBMISSION_ATTRIBUTE_ARTIFACT_NAME = "artifact_name";
    private static final String SUBMISSION_ATTRIBUTE_FILE_PATH = "file_path";
    private static final String SUBMISSION_ATTRIBUTE_CLASSNAME = "classname";

    private SparkSubmitModel submitModel;

    public RemoteDebugRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @NotNull RunConfigurationModule configurationModule, String name) {
        super(name, configurationModule, factory);

        this.submitModel = new SparkSubmitModel(project);
    }

    @Override
    public void readExternal(Element rootElement) throws InvalidDataException {
        super.readExternal(rootElement);

        SparkSubmitModel model = getSubmitModel();
        SparkSubmissionParameter parameter = Optional.ofNullable(model.getSubmissionParameter())
                .orElse(new SparkSubmissionParameter(
                        "",
                        false,
                        "",
                        "",
                        "",
                        "",
                        new ArrayList<String>(),
                        new ArrayList<String>(),
                        new ArrayList<String>(),
                        Arrays.stream(SparkSubmissionParameter.defaultParameters)
                            .collect(Collectors.toMap(Pair::first, Pair::second))));

        Optional.ofNullable(rootElement.getChild(SUBMISSION_CONTENT_NAME)).ifPresent((element -> {
            Optional.ofNullable(element.getAttribute(SUBMISSION_ATTRIBUTE_CLUSTER_NAME))
                    .ifPresent(attribute -> parameter.setClusterName(attribute.getValue()));
            Optional.ofNullable(element.getAttribute(SUBMISSION_ATTRIBUTE_IS_LOCAL_ARTIFACT))
                    .ifPresent(attribute -> parameter.setLocalArtifact(attribute.getValue().toLowerCase()                                                              .equals("true")));
            Optional.ofNullable(element.getAttribute(SUBMISSION_ATTRIBUTE_ARTIFACT_NAME))
                    .ifPresent(attribute -> parameter.setArtifactName(attribute.getValue()));
            Optional.ofNullable(element.getAttribute(SUBMISSION_ATTRIBUTE_CLASSNAME))
                    .ifPresent(attribute -> parameter.setClassName(attribute.getValue()));

            model.setSubmissionParameters(parameter);
        }));
    }

    @Override
    public void writeExternal(Element rootElement) throws WriteExternalException {
        super.writeExternal(rootElement);

        SparkSubmitModel model = getSubmitModel();

        SparkSubmissionParameter submissionParameter = model.getSubmissionParameter();

        if (submissionParameter == null) {
            return;
        }

        // The element to save editor's setting
        Element remoteDebugSettingsElement = new Element(SUBMISSION_CONTENT_NAME);
        remoteDebugSettingsElement.setAttribute(
                SUBMISSION_ATTRIBUTE_CLUSTER_NAME, submissionParameter.getClusterName());
        remoteDebugSettingsElement.setAttribute(
                SUBMISSION_ATTRIBUTE_IS_LOCAL_ARTIFACT, Boolean.toString(model.isLocalArtifact()));
        remoteDebugSettingsElement.setAttribute(
                SUBMISSION_ATTRIBUTE_ARTIFACT_NAME, submissionParameter.getArtifactName());
        remoteDebugSettingsElement.setAttribute(
                SUBMISSION_ATTRIBUTE_CLASSNAME, submissionParameter.getMainClassName());

        rootElement.addContent(remoteDebugSettingsElement);
    }

    public SparkSubmitModel getSubmitModel() {
        return submitModel;
    }

    public Object getSelectedClusterItem() {
        return this.getSubmitModel().getClusterComboBoxModel().getSelectedItem();
    }

    public void setSelectedClusterItem(Object selectedClusterItem) {
        this.getSubmitModel().getClusterComboBoxModel().setSelectedItem(selectedClusterItem);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new RemoteDebugSettingsEditor(this);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {

    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        return new SparkBatchJobSubmissionState(
                getProject(),
                getSubmitModel());
    }

    @Override
    public Collection<Module> getValidModules() {
        return new ArrayList<>();
    }

    public void apply(SparkSubmissionContentPanel submissionPanel) {
        this.submitModel.setSubmissionParameters(submissionPanel.constructSubmissionParameter());
    }
}

