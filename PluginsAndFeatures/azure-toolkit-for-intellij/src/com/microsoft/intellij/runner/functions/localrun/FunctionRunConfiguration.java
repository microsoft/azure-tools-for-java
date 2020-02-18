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
 *
 */

package com.microsoft.intellij.runner.functions.localrun;

import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.runner.AzureRunConfigurationBase;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.Map;

public class FunctionRunConfiguration extends AzureRunConfigurationBase<FunctionRunModel> {

    private JsonObject appSettingsJsonObject;
    private FunctionRunModel functionRunModel;

    private Module module;

    protected FunctionRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        this.functionRunModel = new FunctionRunModel(project);
    }

    @Override
    public FunctionRunModel getModel() {
        return functionRunModel;
    }

    @Override
    public String getTargetName() {
        return "null";
    }

    @Override
    public String getTargetPath() {
        return "null";
    }

    @Override
    public String getSubscriptionId() {
        return "null";
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new FunctionRunSettingEditor(getProject(), this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        return new FunctionRunState(getProject(), this, executor);
    }

    public Module getModule() {
        return module == null ? FunctionUtils.getFunctionModuleByFilePath(getProject(), getModuleFilePath()) : module;
    }

    public JsonObject getAppSettingsJsonObject() {
        return appSettingsJsonObject;
    }

    public String getDebugOptions() {
        return functionRunModel.getDebugOptions();
    }

    public void setDebugOptions(String debugOptions) {
        functionRunModel.setDebugOptions(debugOptions);
    }

    public String getStagingFolder() {
        return functionRunModel.getStagingFolder();
    }

    public String getFuncPath() {
        return functionRunModel.getFuncPath();
    }

    public String getHostJsonPath() {
        return functionRunModel.getHostJsonPath();
    }

    public String getLocalSettingsJsonPath() {
        final String path = functionRunModel.getLocalSettingsJsonPath();
        return StringUtils.isNotEmpty(path) ? path : Paths.get(getProject().getBasePath(), "local.settings.json").toString();
    }

    public Map<String, String> getAppSettings() {
        return functionRunModel.getAppSettings();
    }

    public String getModuleFilePath() {
        return functionRunModel.getModuleFilePath();
    }

    public FunctionRunModel getFunctionRunModel() {
        return functionRunModel;
    }

    public void setLocalSettingsJsonPath(String localSettingsJsonPath) {
        functionRunModel.setLocalSettingsJsonPath(localSettingsJsonPath);
    }

    public void setAppSettings(Map<String, String> appSettings) {
        functionRunModel.setAppSettings(appSettings);
    }

    public void setModuleFilePath(String moduleName) {
        functionRunModel.setModuleFilePath(moduleName);
    }

    public void setFunctionRunModel(FunctionRunModel functionRunModel) {
        this.functionRunModel = functionRunModel;
    }

    public void setAppSettingsJsonObject(JsonObject appSettingsJsonObject) {
        this.appSettingsJsonObject = appSettingsJsonObject;
    }

    public void setModule(Module module) {
        this.module = module;
        this.setModuleFilePath(module.getModuleFilePath());
    }

    public void setStagingFolder(String stagingFolder) {
        functionRunModel.setStagingFolder(stagingFolder);
    }

    public void setFuncPath(String funcPath) {
        functionRunModel.setFuncPath(funcPath);
    }

    public void setHostJsonPath(String hostJsonPath) {
        functionRunModel.setHostJsonPath(hostJsonPath);
    }

    @Override
    public void validate() throws ConfigurationException {

    }
}
