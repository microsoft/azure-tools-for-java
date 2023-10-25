package com.microsoft.azure.toolkit.intellij.storage.completion.resource;

import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Utils {
    public static List<StorageAccount> getConnectedStorageAccounts(@Nonnull final Module module) {
        return Optional.of(module).map(AzureModule::from)
                .map(AzureModule::getDefaultProfile).map(Profile::getConnectionManager).stream()
                .flatMap(m -> m.getConnections().stream())
                .filter(c -> c.getDefinition().getResourceDefinition() instanceof StorageAccountResourceDefinition)
                .map(Connection::getResource)
                .filter(Resource::isValidResource)
                .map(r -> ((StorageAccount) r.getData()))
                .toList();
    }

    public static List<Connection<?, ?>> getConnectionWithStorageAccount(@Nonnull final StorageAccount account, @Nonnull final Module module) {
        return Optional.of(module).map(AzureModule::from)
                .map(AzureModule::getDefaultProfile).map(Profile::getConnectionManager).stream()
                .flatMap(m -> m.getConnections().stream())
                .filter(c -> c.getDefinition().getResourceDefinition() instanceof StorageAccountResourceDefinition)
                .filter(c -> Objects.equals(c.getResource().getData(), account))
                .toList();
    }
}
