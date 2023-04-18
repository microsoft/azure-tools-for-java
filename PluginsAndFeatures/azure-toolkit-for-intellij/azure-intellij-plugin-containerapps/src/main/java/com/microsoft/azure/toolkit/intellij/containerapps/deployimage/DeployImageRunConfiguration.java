/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.deployimage;

import com.intellij.configurationStore.XmlSerializer;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfiguration;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.IDockerConfiguration;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import java.util.Optional;

public class DeployImageRunConfiguration extends LocatableConfigurationBase<Element> implements LocatableConfiguration, IDockerConfiguration {
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
    public void readExternal(org.jdom.@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        this.dataModel = Optional.ofNullable(element.getChild("SpringCloudAppConfig"))
                .map(e -> XmlSerializer.deserialize(e, DeployImageModel.class))
                .orElse(DeployImageModel.builder().build());
//        Optional.ofNullable(element.getChild("Registry"))
//                .map(e -> e.getAttributeValue("id"))
//                .map(id -> (ContainerRegistry)Azure.az(AzureContainerRegistry.class).getById(id))
//                .ifPresent(this.dataModel::setContainerRegistryId);
    }

    @Override
    public void writeExternal(org.jdom.@NotNull Element element) {
        super.writeExternal(element);
        Optional.ofNullable(this.dataModel)
                .map(config -> XmlSerializer.serialize(config, (accessor, o) -> !"containerRegistry".equalsIgnoreCase(accessor.getName())))
                .ifPresent(element::addContent);
//        Optional.ofNullable(this.dataModel)
//                .map(DeployImageModel::getContainerRegistryId)
//                .map(ContainerRegistry::getId)
//                .map(id -> new org.jdom.Element("Registry").setAttribute("id", id))
//                .ifPresent(element::addContent);
    }
}
