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

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.execution.scratch.JavaScratchConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJobConfigurableModel;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobSubmissionState;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionContentPanel;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class RemoteDebugRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
{
    @NotNull
    private SparkBatchJobConfigurableModel jobModel;

    public RemoteDebugRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @NotNull RunConfigurationModule configurationModule, String name) {
        super(name, configurationModule, factory);

        this.jobModel = new SparkBatchJobConfigurableModel(project);
    }

    @Override
    public void readExternal(Element rootElement) throws InvalidDataException {
        super.readExternal(rootElement);

        jobModel.applyFromElement(rootElement);
    }

    @Override
    public void writeExternal(Element rootElement) throws WriteExternalException {
        super.writeExternal(rootElement);

        Element jobConfigElement = jobModel.exportToElement();
        rootElement.addContent(jobConfigElement);
    }

    @NotNull
    public SparkBatchJobConfigurableModel getModel() {
        return jobModel;
    }

    @NotNull
    public SparkSubmitModel getSubmitModel() {
        return getModel().getSubmitModel();
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
        return new SparkBatchJobSubmissionState(getProject(), jobModel);
    }

    @Override
    public Collection<Module> getValidModules() {
        return new ArrayList<>();
    }

    public void setAsNew() {
    }
}

