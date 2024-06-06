/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.deployimage;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.util.xmlb.Accessor;
import com.intellij.util.xmlb.SerializationFilterBase;
import com.intellij.util.xmlb.XmlSerializer;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.IDockerPushConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DeployImageRunConfiguration extends LocatableConfigurationBase<Element> implements LocatableConfiguration, IDockerPushConfiguration {
    @Getter
    @Setter
    private DeployImageModel dataModel;

    protected DeployImageRunConfiguration(@Nonnull Project project, @Nonnull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        dataModel = new DeployImageModel();
    }

    @Nonnull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DeployImageRunSettingsEditor(this.getProject(), this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment executionEnvironment) {
        return new DeployImageRunState(getProject(), this);
    }

    @Nullable
    public DockerImage getDockerImageConfiguration() {
        return Optional.ofNullable(dataModel).map(DeployImageModel::getDockerImage).orElse(null);
    }

    @Nullable
    @Override
    public DockerHost getDockerHostConfiguration() {
        return Optional.ofNullable(dataModel).map(DeployImageModel::getDockerHost).orElse(null);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        final List<Element> models = element.getChildren("DeployImageModel");
        final Element source = CollectionUtils.isNotEmpty(models) ? models.get(0) : element;
        try {
            XmlSerializer.deserializeInto(dataModel, source);
        } catch (final Error e) {
            // swallow exception during deserialization
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        if (Objects.nonNull(this.dataModel)) {
            XmlSerializer.serializeInto(this.dataModel, element, new SerializationFilterBase() {
                @Override
                protected boolean accepts(@Nonnull Accessor accessor, @Nonnull Object bean, @Nullable Object beanValue) {
                    return !StringUtils.equalsAnyIgnoreCase(accessor.getName(), "containerRegistry", "ImageConfig", "ContainerAppConfig");
                }
            });
        }
    }

    @Override
    public String getContainerRegistryId() {
        return getDataModel().getContainerRegistryId();
    }

    @Override
    public String getFinalRepositoryName() {
        return getDataModel().getFinalRepositoryName();
    }

    @Override
    public String getFinalTagName() {
        return getDataModel().getFinalTagName();
    }

    public void setFinalRepositoryName(final String value) {
        getDataModel().setFinalRepositoryName(value);
    }

    public void setFinalTagName(final String value) {
        getDataModel().setFinalTagName(value);
    }

    public void setModuleName(final String moduleName){
        getDataModel().setModuleName(moduleName);
    }

    public String getModuleName() {
        return getDataModel().getModuleName();
    }

    public void setDeploymentType(final DeploymentType type){
        getDataModel().setDeploymentType(type);
    }

    public DeploymentType getDeploymentType() {
        return getDataModel().getDeploymentType();
    }
}
