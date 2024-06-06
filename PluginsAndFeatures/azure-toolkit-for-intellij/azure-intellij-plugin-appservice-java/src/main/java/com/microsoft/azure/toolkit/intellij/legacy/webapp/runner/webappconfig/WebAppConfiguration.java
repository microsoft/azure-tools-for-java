/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactType;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import com.microsoft.azure.toolkit.intellij.connector.IConnectionAware;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.DeploymentSlotConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class WebAppConfiguration extends AzureRunConfigurationBase<IntelliJWebAppSettingModel>
    implements IWebAppRunConfiguration, IConnectionAware {

    // const string
    private static final String SLOT_NAME_REGEX = "[a-zA-Z0-9-]{1,60}";
    private static final String TOMCAT = "tomcat";
    private static final String JAVA = "java";
    private static final String JBOSS = "jboss";
    public static final String JAVA_VERSION = "javaVersion";
    @Getter
    private final IntelliJWebAppSettingModel webAppSettingModel;

    public WebAppConfiguration(@Nonnull Project project, @Nonnull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        this.webAppSettingModel = new IntelliJWebAppSettingModel();
    }

    @Override
    public IntelliJWebAppSettingModel getModel() {
        return this.webAppSettingModel;
    }

    @Nonnull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WebAppSettingEditor(getProject(), this);
    }

    @Nullable
    public Module getModule() {
        final AzureArtifact azureArtifact = AzureArtifactManager.getInstance(this.getProject())
                                                                .getAzureArtifactById(this.getAzureArtifactType(), this.getArtifactIdentifier());
        return Optional.ofNullable(azureArtifact).map(AzureArtifact::getModule).orElse(null);
    }

    @Nullable
    @Override
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment executionEnvironment) {
        return new WebAppRunState(getProject(), this);
    }

    @Override
    public void readExternal(final Element element) throws InvalidDataException {
        super.readExternal(element);
        Optional.ofNullable(element.getChild(JAVA_VERSION))
                .map(javaVersionElement -> javaVersionElement.getAttributeValue(JAVA_VERSION))
                .ifPresent(webAppSettingModel::setWebAppJavaVersion);
        Optional.ofNullable(this.getAppSettingsKey())
                .ifPresent(key -> webAppSettingModel.getConfig().setAppSettings(FunctionUtils.loadAppSettingsFromSecurityStorage(getAppSettingsKey())));
    }

    @Override
    public void validate() throws ConfigurationException {
        final AppServiceConfig config = this.webAppSettingModel.getConfig();
        if (StringUtils.isEmpty(config.getAppName())) {
            throw new ConfigurationException(message("webapp.deploy.validate.noWebAppName"));
        }
        if (StringUtils.isEmpty(config.getSubscriptionId())) {
            throw new ConfigurationException(message("webapp.deploy.validate.noSubscription"));
        }
        if (StringUtils.isEmpty(config.getResourceGroup())) {
            throw new ConfigurationException(message("webapp.deploy.validate.noResourceGroup"));
        }
        if (StringUtils.isEmpty(config.getServicePlanName())) {
            throw new ConfigurationException(message("webapp.deploy.validate.noAppServicePlan"));
        }
        final DeploymentSlotConfig slotConfig = config.getSlotConfig();
        if (Objects.nonNull(slotConfig)) {
            if (StringUtils.isEmpty(slotConfig.getName())) {
                throw new ConfigurationException(message("webapp.deploy.validate.noSlotName"));
            }
            if (!slotConfig.getName().matches(SLOT_NAME_REGEX)) {
                throw new ConfigurationException(message("webapp.deploy.validate.invalidSlotName"));
            }
        }
        // validate runtime with artifact
        final WebAppRuntime runtime;
        try {
            runtime = (WebAppRuntime) RuntimeConfig.toWebAppRuntime(config.getRuntime());
        } catch (final RuntimeException e) {
            throw new ConfigurationException(message("webapp.validate_deploy_configuration.invalidRuntime"));
        }
        final OperatingSystem operatingSystem = runtime.getOperatingSystem();
        final JavaVersion javaVersion = runtime.getJavaVersion();
        if (operatingSystem == OperatingSystem.DOCKER) {
            throw new ConfigurationException(message("webapp.validate_deploy_configuration.dockerRuntime"));
        }
        if (Objects.equals(javaVersion, JavaVersion.OFF)) {
            throw new ConfigurationException(message("webapp.validate_deploy_configuration.invalidRuntime"));
        }
        final String containerName = runtime.getContainerName();
        final String artifactPackage = webAppSettingModel.getPackaging();
        if (StringUtils.containsIgnoreCase(containerName, TOMCAT) && !StringUtils.equalsAnyIgnoreCase(artifactPackage, "war")) {
            throw new ConfigurationException(message("webapp.deploy.validate.invalidTomcatArtifact"));
        } else if (StringUtils.containsIgnoreCase(containerName, JBOSS) && !StringUtils.equalsAnyIgnoreCase(artifactPackage, "war", "ear")) {
            throw new ConfigurationException(message("webapp.deploy.validate.invalidJbossArtifact"));
        } else if (StringUtils.containsIgnoreCase(containerName, "Java") && !StringUtils.equalsAnyIgnoreCase(artifactPackage, "jar")) {
            throw new ConfigurationException(message("webapp.deploy.validate.invalidJavaSeArtifact"));
        }
        if (StringUtils.isEmpty(webAppSettingModel.getArtifactIdentifier())) {
            throw new ConfigurationException(message("webapp.deploy.validate.missingArtifact"));
        }
    }

    @Override
    public String getTargetPath() {
        return webAppSettingModel.getTargetPath();
    }

    @Override
    public String getSubscriptionId() {
        return webAppSettingModel.getConfig().subscriptionId();
    }

    @Override
    public String getTargetName() {
        return webAppSettingModel.getTargetName();
    }

    public void setOpenBrowserAfterDeployment(boolean openBrowserAfterDeployment) {
        webAppSettingModel.setOpenBrowserAfterDeployment(openBrowserAfterDeployment);
    }

    public AzureArtifactType getAzureArtifactType() {
        return webAppSettingModel.getAzureArtifactType();
    }

    public String getArtifactIdentifier() {
        return webAppSettingModel.getArtifactIdentifier();
    }

    public void setWebApp(@Nonnull WebAppBase<?,?,?> webApp) {
        final AppServiceConfig config = AppServiceConfigUtils.fromAppService(webApp);
        this.webAppSettingModel.setConfig(config);
        Optional.ofNullable(webApp.getAppSettings()).ifPresent(values -> this.saveAppSettings(getAppSettingsKey(), values));
    }

    public void setArtifact(AzureArtifact azureArtifact) {
        final AzureArtifactManager azureArtifactManager = AzureArtifactManager.getInstance(getProject());
        webAppSettingModel.setArtifactIdentifier(azureArtifact == null ? null : azureArtifact.getIdentifier());
        webAppSettingModel.setAzureArtifactType(azureArtifact == null ? null : azureArtifact.getType());
        webAppSettingModel.setPackaging(azureArtifact == null ? null : azureArtifact.getPackaging());
    }

    public void setWebAppSettingModel(@Nonnull final IntelliJWebAppSettingModel value) {
        this.saveAppSettings(value.getAppSettingsKey(), value.getConfig().appSettings());
        this.webAppSettingModel.setConfig(value.getConfig());
        this.webAppSettingModel.setDeployToRoot(value.isDeployToRoot());
        this.webAppSettingModel.setAzureArtifactType(value.getAzureArtifactType());
        this.webAppSettingModel.setOpenBrowserAfterDeployment(value.isOpenBrowserAfterDeployment());
        this.webAppSettingModel.setSlotPanelVisible(value.isSlotPanelVisible());
        this.webAppSettingModel.setArtifactIdentifier(value.getArtifactIdentifier());
        this.webAppSettingModel.setPackaging(value.getPackaging());
    }

    public AppServiceConfig getAppServiceConfig() {
        final AppServiceConfig config = getModel().getConfig();
        config.setAppSettings(FunctionUtils.loadAppSettingsFromSecurityStorage(getAppSettingsKey()));
        return config;
    }

    public void saveAppSettings(@Nonnull final String appSettingsKey, @Nonnull final Map<String, String> appSettings) {
        this.webAppSettingModel.setAppSettingsKey(appSettingsKey);
        this.webAppSettingModel.getConfig().setAppSettings(appSettings);
        FunctionUtils.saveAppSettingsToSecurityStorage(appSettingsKey, appSettings);
    }

    public String getAppSettingsKey() {
        return webAppSettingModel.getAppSettingsKey();
    }
}
