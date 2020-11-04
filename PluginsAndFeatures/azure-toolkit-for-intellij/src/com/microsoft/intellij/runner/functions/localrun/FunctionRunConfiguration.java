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

package com.microsoft.intellij.runner.functions.localrun;

import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.LocatableConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.runner.AzureRunConfigurationBase;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public class FunctionRunConfiguration extends AzureRunConfigurationBase<FunctionRunModel>
    implements LocatableConfiguration, RunProfileWithCompileBeforeLaunchOption {
    private JsonObject appSettingsJsonObject;
    private FunctionRunModel functionRunModel;

    protected FunctionRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        this.functionRunModel = new FunctionRunModel();
        this.myModule = new JavaRunConfigurationModule(project, true);
    }

    @NotNull
    @Override
    public Module[] getModules() {
        final Module module = getModule();
        return module == null ? Module.EMPTY_ARRAY : new Module[] { module };
    }

    public Module getModule() {
        Module module = ReadAction.compute(() -> getConfigurationModule().getModule());
        if (module == null && StringUtils.isNotEmpty(this.functionRunModel.getModuleName())) {
            module = FunctionUtils.getFunctionModuleByName(getProject(), this.functionRunModel.getModuleName());
            this.myModule.setModule(module);
        }
        return module;
    }

    @Override
    public FunctionRunModel getModel() {
        return functionRunModel;
    }

    @Override
    public String getTargetName() {
        return "untitled";
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

    public String getModuleName() {
        return functionRunModel.getModuleName();
    }

    public String getLocalSettingsJsonPath() {
        final String path = functionRunModel.getLocalSettingsJsonPath();
        return StringUtils.isNotEmpty(path) ? path : Paths.get(getProject().getBasePath(), "local.settings.json").toString();
    }

    public Map<String, String> getAppSettings() {
        return functionRunModel.getAppSettings();
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

    public void setFunctionRunModel(FunctionRunModel functionRunModel) {
        this.functionRunModel = functionRunModel;
    }

    public void setAppSettingsJsonObject(JsonObject appSettingsJsonObject) {
        this.appSettingsJsonObject = appSettingsJsonObject;
    }

    public void saveModule(Module module) {
        if (module == null) {
            return;
        }
        this.functionRunModel.setModuleName(module.getName());
        this.myModule.setModule(module);
    }

    public void initializeDefaults(Module module) {
        if (module == null) {
            return;
        }
        saveModule(module);

        if (StringUtils.isEmpty(this.getFuncPath())) {
            try {
                this.setFuncPath(FunctionUtils.getFuncPath());
            } catch (IOException | InterruptedException ex) {
                // ignore;
            }
        }
        if (StringUtils.isEmpty(this.getStagingFolder())) {
            this.setStagingFolder(FunctionUtils.getTargetFolder(module));
        }

        if (StringUtils.isEmpty(this.getHostJsonPath())) {
            this.setHostJsonPath(Paths.get(getProject().getBasePath(), "host.json").toString());
        }

        if (StringUtils.isEmpty(this.getLocalSettingsJsonPath())) {
            this.setLocalSettingsJsonPath(Paths.get(getProject().getBasePath(), "local.settings.json").toString());
        }
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
        if (getModule() == null) {
            throw new ConfigurationException("Please specify module");
        }

        if (StringUtils.isEmpty(getFuncPath())) {
            throw new ConfigurationException("Please specify function cli path");
        }

        final File func = new File(getFuncPath());
        if (!func.exists() || !func.isFile() || !func.getName().contains("func")) {
            throw new ConfigurationException("Please specify correct function cli path");
        }
    }

    @Override
    public boolean isGeneratedName() {
        return false;
    }

    @Nullable
    @Override
    public String suggestedName() {
        return "Unnamed";
    }
}
