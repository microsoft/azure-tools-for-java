package com.microsoft.azure.toolkit.intellij.storage.code;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionUtils;
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
        return com.microsoft.azure.toolkit.intellij.connector.code.Utils.getConnectedResources(module, StorageAccountResourceDefinition.INSTANCE);
    }

    public static List<Connection<?, ?>> getConnectionWithStorageAccount(@Nonnull final StorageAccount account, @Nonnull final Module module) {
        return Optional.of(module).map(AzureModule::from)
                .map(AzureModule::getDefaultProfile).map(Profile::getConnectionManager).stream()
                .flatMap(m -> m.getConnections().stream())
                .filter(c -> c.getDefinition().getResourceDefinition() instanceof StorageAccountResourceDefinition)
                .filter(c -> Objects.equals(c.getResource().getData(), account))
                .toList();
    }

    public static StorageAccount getBindingStorageAccount(@Nonnull final PsiAnnotation annotation) {
        return Optional.ofNullable(FunctionUtils.getConnectionFromAnnotation(annotation))
                .filter(c -> c.getResource().getData() instanceof StorageAccount)
                .map(c -> (StorageAccount) c.getResource().getData())
                .orElse(null);
    }
}
