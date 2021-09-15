/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionDefinition;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

public class StorageAccountResourceConnection extends Connection<StorageAccount, String> {
    public static final Definition DEFINITION = Definition.MODULE_STORAGE;

    public StorageAccountResourceConnection(AzureServiceResource<StorageAccount> resource, ModuleResource consumer, Definition definition) {
        super(resource, consumer, definition);
    }

    protected Map<String, String> initEnv(@Nonnull final Project project) {
        final StorageAccount account = this.resource.getData();
        final String conString = account.getConnectionString();
        return Collections.singletonMap(this.getEnvPrefix() + "CONNECTION_STRING", conString);
    }

    @Getter
    public static class Definition extends ConnectionDefinition<StorageAccount, String> {
        public static final Definition MODULE_STORAGE = new Definition(StorageAccountResource.DEFINITION, ModuleResource.Definition.IJ_MODULE);

        public Definition(ResourceDefinition<StorageAccount> rd, ResourceDefinition<String> cd) {
            super(rd, cd);
        }

        @Nonnull
        @Override
        public Connection<StorageAccount, String> define(Resource<StorageAccount> resource, Resource<String> consumer) {
            return new StorageAccountResourceConnection((AzureServiceResource<StorageAccount>) resource, (ModuleResource) consumer, this);
        }
    }
}
