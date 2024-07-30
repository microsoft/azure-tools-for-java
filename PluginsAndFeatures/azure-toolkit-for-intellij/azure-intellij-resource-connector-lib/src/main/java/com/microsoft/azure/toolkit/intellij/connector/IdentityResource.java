/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.identities.AzureManagedIdentity;
import com.microsoft.azure.toolkit.lib.identities.Identity;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IdentityResource extends AzureServiceResource<Identity> {
    public IdentityResource(@Nonnull Identity data, @Nonnull AzureServiceResource.Definition<Identity> definition) {
        super(data, definition);
    }

    @Getter
    public static class Definition extends AzureServiceResource.Definition<Identity> {
        public static final Definition INSTANCE = new Definition();

        public Definition() {
            super("Managed Identity", "Managed Identity", AzureIcons.Common.AZURE.getIconPath());
        }

        @Override
        public Resource<Identity> define(Identity resource) {
            return super.define(resource, null);
        }

        @Override
        public Identity getResource(String dataId, final String id) {
            return Azure.az(AzureManagedIdentity.class).getById(dataId);
        }

        @Override
        public Map<String, String> initEnv(AzureServiceResource<Identity> data, Project project) {
            return Collections.emptyMap();
        }

        @Override
        public int getRole() {
            return IDENTITY;
        }

        @Override
        public AzureFormJPanel<Resource<Identity>> getResourcePanel(Project project) {
            throw new AzureToolkitRuntimeException("not supported");
        }

        @Override
        public List<Resource<Identity>> getResources(Project project) {
            throw new AzureToolkitRuntimeException("not supported");
        }
    }
}
