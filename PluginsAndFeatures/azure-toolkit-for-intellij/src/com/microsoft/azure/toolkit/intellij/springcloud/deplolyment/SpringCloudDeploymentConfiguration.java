/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.LocatableConfiguration;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.Accessor;
import com.intellij.util.xmlb.SerializationFilterBase;
import com.intellij.util.xmlb.XmlSerializer;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azuretools.utils.JsonUtils;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public class SpringCloudDeploymentConfiguration extends LocatableConfigurationBase implements LocatableConfiguration {
    private static final String NEED_SPECIFY_ARTIFACT = "Please select an artifact";
    private static final String NEED_SPECIFY_SUBSCRIPTION = "Please select your subscription.";
    private static final String NEED_SPECIFY_CLUSTER = "Please select a target cluster.";
    private static final String NEED_SPECIFY_APP_NAME = "Please select a target app.";
    private static final String SERVICE_IS_NOT_READY = "Service is not ready for deploy, current status is ";
    private static final String TARGET_CLUSTER_DOES_NOT_EXISTS = "Target cluster does not exists.";
    private static final String TARGET_CLUSTER_IS_NOT_AVAILABLE = "Target cluster cannot be found in current subscription";

    @Getter
    private final SpringCloudAppConfig appConfig;

    public SpringCloudDeploymentConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        this.appConfig = SpringCloudAppConfig.builder()
            .deployment(SpringCloudDeploymentConfig.builder().build())
            .build();
    }

    protected SpringCloudDeploymentConfiguration(@NotNull SpringCloudDeploymentConfiguration source) {
        super(source.getProject(), source.getFactory(), source.getName());
        this.appConfig = JsonUtils.deepCopyWithJson(source.appConfig);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        XmlSerializer.deserializeInto(this.appConfig, element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        XmlSerializer.serializeInto(this.appConfig, element, new SerializationFilterBase() {
            @Override
            protected boolean accepts(@NotNull Accessor accessor, @NotNull Object bean, @Nullable Object beanValue) {
                return !"password".equalsIgnoreCase(accessor.getName());
            }
        });
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new Editor(this, getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new SpringCloudDeploymentConfigurationState(getProject(), new SpringCloudDeploymentConfiguration(this));
    }

    @Override
    public void checkConfiguration() {
    }

    static class Factory extends ConfigurationFactory {
        public static final String FACTORY_NAME = "Deploy Spring Cloud Services";

        public Factory(@NotNull ConfigurationType type) {
            super(type);
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new SpringCloudDeploymentConfiguration(project, this, project.getName());
        }

        @Override
        public RunConfiguration createConfiguration(String name, RunConfiguration template) {
            return new SpringCloudDeploymentConfiguration(template.getProject(), this, name);
        }

        @Override
        public String getName() {
            return FACTORY_NAME;
        }
    }

    static class Editor extends SettingsEditor<SpringCloudDeploymentConfiguration> {
        private final SpringCloudDeploymentConfigurationPanel panel;

        Editor(SpringCloudDeploymentConfiguration configuration, Project project) {
            super();
            this.panel = new SpringCloudDeploymentConfigurationPanel(project);
        }

        protected void disposeEditor() {
            super.disposeEditor();
        }

        @Override
        protected void resetEditorFrom(@NotNull SpringCloudDeploymentConfiguration config) {
            this.panel.setData(config.appConfig);
        }

        @Override
        protected void applyEditorTo(@NotNull SpringCloudDeploymentConfiguration config) throws ConfigurationException {
            final List<AzureValidationInfo> infos = this.panel.validateData();
            final AzureValidationInfo error = infos.stream()
                .filter(i -> i.getType() == AzureValidationInfo.Type.ERROR)
                .findAny().orElse(null);
            if (Objects.nonNull(error)) {
                throw new ConfigurationException(error.getMessage());
            }
            this.panel.getData(config.appConfig);
        }

        @Override
        protected @NotNull JComponent createEditor() {
            return this.panel.getContentPanel();
        }
    }
}
