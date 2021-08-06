/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy;

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
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import com.microsoft.azure.toolkit.intellij.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FunctionDeployConfiguration extends AzureRunConfigurationBase<FunctionDeployModel>
    implements RunProfileWithCompileBeforeLaunchOption {

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
        return Optional.ofNullable(functionDeployModel.getFunctionAppConfig())
                .map(FunctionAppConfig::getSubscription).map(Subscription::getId).orElse(StringUtils.EMPTY);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new FunctionDeploymentSettingEditor(getProject(), this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
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

    @Override
    public void validate() throws ConfigurationException {
        checkAzurePreconditions();
        if (this.module == null) {
            throw new ConfigurationException(message("function.deploy.validate.noModule"));
        }
        final FunctionAppConfig functionAppConfig = functionDeployModel.getFunctionAppConfig();
        if (StringUtils.isAllEmpty(functionAppConfig.getResourceId(), functionAppConfig.getName())) {
            throw new ConfigurationException(message("function.deploy.validate.noTarget"));
        }
    }

    public Map<String, String> getAppSettings() {
        return Optional.ofNullable(functionDeployModel.getFunctionAppConfig()).map(FunctionAppConfig::getAppSettings).orElse(Collections.emptyMap());
    }

    public String getAppSettingsKey() {
        return functionDeployModel.getAppSettingsKey();
    }

    public String getFunctionId() {
        return Optional.ofNullable(functionDeployModel.getFunctionAppConfig()).map(FunctionAppConfig::getResourceId).orElse(StringUtils.EMPTY);
    }

    public String getAppName() {
        return Optional.ofNullable(functionDeployModel.getFunctionAppConfig()).map(FunctionAppConfig::getName).orElse(StringUtils.EMPTY);
    }

    public FunctionAppConfig getConfig() {
        return functionDeployModel.getFunctionAppConfig();
    }

    public void saveConfig(FunctionAppConfig config) {
        functionDeployModel.setFunctionAppConfig(config);
    }

    public void setAppSettingsKey(String appSettingsKey) {
        functionDeployModel.setAppSettingsKey(appSettingsKey);
    }

    public void setFunctionId(String id) {
        functionDeployModel.getFunctionAppConfig().setResourceId(id);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        this.functionDeployModel = Optional.ofNullable(element.getChild("FunctionDeployModel"))
                .map(e -> XmlSerializer.deserialize(e, FunctionDeployModel.class))
                .orElseGet(() -> Optional.of(element)
                        .map(e -> XmlSerializer.deserialize(e, FunctionDeployModel.DeprecatedDeployModel.class))
                        .map(FunctionDeployModel::new)
                        .orElse(new FunctionDeployModel()));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        Optional.ofNullable(this.functionDeployModel)
                .map(config -> XmlSerializer.serialize(config, (accessor, o) -> !"appSettings".equalsIgnoreCase(accessor.getName())))
                .ifPresent(element::addContent);
    }
}
