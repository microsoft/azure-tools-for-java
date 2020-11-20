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

package com.microsoft.intellij.runner.functions.deploy;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBoxModel;
import com.microsoft.intellij.runner.AzureRunConfigurationBase;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FunctionDeployConfiguration extends AzureRunConfigurationBase<FunctionDeployModel>
    implements RunProfileWithCompileBeforeLaunchOption {

    public static final String NEED_SPECIFY_MODULE = "Please specify module";
    public static final String NEED_SPECIFY_TARGET_FUNCTION = "Please specify target function";
    public static final String NEED_SPECIFY_VALID_STAGING_DIRECTORY_PATH = "Please specify valid staging directory path";
    private FunctionDeployModel functionDeployModel;
    private Module module;

    public FunctionDeployConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        functionDeployModel = new FunctionDeployModel();
    }

    @NotNull
    @Override
    public Module[] getModules() {
        return ModuleManager.getInstance(getProject()).getModules();
    }

    public String getSubscription() {
        return functionDeployModel.getSubscription();
    }

    public String getResourceGroup() {
        return functionDeployModel.getResourceGroup();
    }

    public RuntimeConfiguration getRuntime() {
        return functionDeployModel.getRuntime();
    }

    public String getRegion() {
        return functionDeployModel.getRegion();
    }

    public String getFunctionId() {
        return functionDeployModel.getFunctionId();
    }

    public Map getAppSettings() {
        return functionDeployModel.getAppSettings();
    }

    public void setSubscription(String subscription) {
        functionDeployModel.setSubscription(subscription);
    }

    public void setResourceGroup(String resourceGroup) {
        functionDeployModel.setResourceGroup(resourceGroup);
    }

    public String getAppName() {
        return functionDeployModel.getAppName();
    }

    public void setAppName(String appName) {
        functionDeployModel.setAppName(appName);
    }

    public void setRegion(String region) {
        functionDeployModel.setRegion(region);
    }

    public void setPricingTier(String pricingTier) {
        functionDeployModel.setPricingTier(pricingTier);
    }

    public void setFunctionId(String functionId) {
        functionDeployModel.setFunctionId(functionId);
    }

    public void setAppSettings(Map<String, String> appSettings) {
        functionDeployModel.setAppSettings(appSettings);
    }

    public void setTargetFunction(FunctionApp targetFunction) {
        if (targetFunction == null) {
            return;
        }
        setAppName(targetFunction.name());
        setFunctionId(targetFunction.id());
        setResourceGroup(targetFunction.resourceGroupName());
    }

    @Override
    public FunctionDeployModel getModel() {
        return this.functionDeployModel;
    }

    @Override
    public String getTargetName() {
        return null;
    }

    @Override
    public String getTargetPath() {
        return null;
    }

    @Override
    public String getSubscriptionId() {
        return functionDeployModel.getSubscription();
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new FunctionDeploymentSettingEditor(getProject(), this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new FunctionDeploymentState(getProject(), this);
    }

    public void setDeploymentStagingDirectory(String deploymentStagingDirectory) {
        this.functionDeployModel.setDeploymentStagingDirectoryPath(deploymentStagingDirectory);
    }

    public String getDeploymentStagingDirectory() {
        return this.functionDeployModel.getDeploymentStagingDirectoryPath();
    }

    public Module getModule() {
        return module == null ? FunctionUtils.getFunctionModuleByName(getProject(), functionDeployModel.getModuleName()) : module;
    }

    public void saveTargetModule(Module module) {
        if (module != null) {
            this.module = module;
            functionDeployModel.setModuleName(module.getName());
        }
    }

    public boolean isNewResource() {
        return functionDeployModel.isNewResource();
    }

    public void setNewResource(final boolean newResource) {
        functionDeployModel.setNewResource(newResource);
    }

    public void setAppServicePlanName(final String name) {
        functionDeployModel.setAppServicePlanName(name);
    }

    public void setAppServicePlanResourceGroup(final String resourceGroupName) {
        functionDeployModel.setAppServicePlanResourceGroup(resourceGroupName);
    }

    public void setFunctionDeployModel(final FunctionDeployModel functionDeployModel) {
        this.functionDeployModel = functionDeployModel;
    }

    public String getOs() {
        return functionDeployModel.getOs();
    }

    public void setOs(final String os) {
        functionDeployModel.setOs(os);
    }

    public String getJavaVersion() {
        return functionDeployModel.getJavaVersion();
    }

    public void setJavaVersion(final String javaVersion) {
        functionDeployModel.setJavaVersion(javaVersion);
    }

    public void setInstrumentationKey(String instrumentationKey) {
        this.functionDeployModel.setInstrumentationKey(instrumentationKey);
    }

    public String getInstrumentationKey() {
        return functionDeployModel.getInstrumentationKey();
    }

    public void setInsightsName(String insightsName) {
        this.functionDeployModel.setInsightsName(insightsName);
    }

    public String getInsightsName() {
        return functionDeployModel.getInsightsName();
    }

    public void saveModel(FunctionAppComboBoxModel functionAppComboBoxModel) {
        if (functionAppComboBoxModel.getFunctionDeployModel() != null) {
            setFunctionDeployModel(functionAppComboBoxModel.getFunctionDeployModel());
            return;
        } else {
            functionDeployModel.saveModel(functionAppComboBoxModel);
        }
    }

    @Override
    public void validate() throws ConfigurationException {
        checkAzurePreconditions();
        if (this.module == null) {
            throw new ConfigurationException(NEED_SPECIFY_MODULE);
        }
        if (StringUtils.isEmpty(this.getFunctionId()) && !isNewResource()) {
            throw new ConfigurationException(NEED_SPECIFY_TARGET_FUNCTION);
        }
    }

}
