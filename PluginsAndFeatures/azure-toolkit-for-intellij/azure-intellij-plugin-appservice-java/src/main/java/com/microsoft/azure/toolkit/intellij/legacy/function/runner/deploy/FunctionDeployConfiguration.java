/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.IConnectionAware;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class FunctionDeployConfiguration extends AzureRunConfigurationBase<FunctionDeployModel>
        implements RunProfileWithCompileBeforeLaunchOption, IConnectionAware {

    private FunctionDeployModel functionDeployModel;
    private Module module;
    @Getter
    private List<Connection<?, ?>> connections = new ArrayList<>();

    public FunctionDeployConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        functionDeployModel = new FunctionDeployModel();
    }

    @Override
    public void setConnection(@Nonnull Connection<?, ?> connection) {
        addConnection(connection);
    }

    @Override
    public void addConnection(@Nonnull Connection<?, ?> connection) {
        connections.add(connection);
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

    @Override
    public Module getModule() {
        Module module = ReadAction.compute(() -> getConfigurationModule().getModule());
        if (module == null && StringUtils.isNotEmpty(this.functionDeployModel.getModuleName())) {
            module = Arrays.stream(ModuleManager.getInstance(getProject()).getModules())
                    .filter(m -> StringUtils.equals(this.functionDeployModel.getModuleName(), m.getName()))
                    .findFirst().orElse(null);
            this.myModule.setModule(module);
        }
        return module;
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
        final Runtime runtime = functionAppConfig.getRuntime();
        final OperatingSystem operatingSystem = Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).orElse(null);
        final JavaVersion javaVersion = Optional.ofNullable(runtime).map(Runtime::getJavaVersion).orElse(null);
        if (operatingSystem == OperatingSystem.DOCKER) {
            throw new ConfigurationException(message("function.validate_deploy_configuration.dockerRuntime"));
        }
        if (functionAppConfig.getServicePlan() == null) {
            // Service plan could be null as lazy loading, throw exception in this case
            throw new ConfigurationException(message("function.validate_deploy_configuration.loading"));
        }
        if (javaVersion == null || Objects.equals(javaVersion, JavaVersion.OFF)) {
            throw new ConfigurationException(message("function.validate_deploy_configuration.invalidRuntime"));
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
        FunctionUtils.saveAppSettingsToSecurityStorage(getAppSettingsKey(), config.getAppSettings());
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
                .map(e -> {
                    try {
                        return XmlSerializer.deserialize(e, FunctionDeployModel.class);
                    } catch (final Throwable t) {
                        return null;
                    }
                })
                .orElseGet(FunctionDeployModel::new);
        Optional.ofNullable(this.getAppSettingsKey())
                .ifPresent(key -> functionDeployModel.getFunctionAppConfig().setAppSettings(FunctionUtils.loadAppSettingsFromSecurityStorage(getAppSettingsKey())));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        Optional.ofNullable(this.functionDeployModel)
                .map(config -> XmlSerializer.serialize(config, (accessor, o) -> !"appSettings".equalsIgnoreCase(accessor.getName())))
                .ifPresent(element::addContent);
    }

    public void setAppSettings(Map<String, String> appSettings) {
        functionDeployModel.getFunctionAppConfig().setAppSettings(appSettings);
        functionDeployModel.setAppSettingsHash(DigestUtils.md5Hex(JsonUtils.toJson(appSettings)).toUpperCase());
        FunctionUtils.saveAppSettingsToSecurityStorage(getAppSettingsKey(), appSettings);
    }
}
