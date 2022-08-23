/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.common;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.LocatableConfiguration;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.Accessor;
import com.intellij.util.xmlb.SerializationFilterBase;
import com.intellij.util.xmlb.XmlSerializer;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.intellij.util.AzureLoginHelper;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AzureRunConfigurationBase<T> extends LocatableConfigurationBase implements LocatableConfiguration {
    private boolean firstTimeCreated = true;
    protected JavaRunConfigurationModule myModule;

    protected AzureRunConfigurationBase(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    protected AzureRunConfigurationBase(@NotNull AzureRunConfigurationBase source) {
        super(source.getProject(), source.getFactory(), source.getName());
    }

    public abstract T getModel();

    public abstract String getTargetName();

    public abstract String getTargetPath();

    public abstract String getSubscriptionId();

    public abstract void validate() throws ConfigurationException;

    public final boolean isFirstTimeCreated() {
        return firstTimeCreated;
    }

    public final void setFirstTimeCreated(boolean firstTimeCreated) {
        this.firstTimeCreated = firstTimeCreated;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        firstTimeCreated = Comparing.equal(element.getAttributeValue("default"), "true");
        XmlSerializer.deserializeInto(getModel(), element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        XmlSerializer.serializeInto(getModel(), element, new SerializationFilterBase() {
            @Override
            protected boolean accepts(@NotNull Accessor accessor, @NotNull Object bean, @Nullable Object beanValue) {
                return !StringUtils.equalsAnyIgnoreCase(accessor.getName(), "password", "appSettings");
            }
        });
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
    }

    public JavaRunConfigurationModule getConfigurationModule() {
        return myModule;
    }

    protected void checkAzurePreconditions() throws ConfigurationException {
        try {
            AzureLoginHelper.ensureAzureSubsAvailable();
        } catch (AzureExecutionException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    public String getArtifactIdentifier() {
        return null;
    }
}
