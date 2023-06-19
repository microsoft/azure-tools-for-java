package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DotEnvBeforeRunTaskProvider;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.facet.AzureProjectFacet;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface IConnectionAware extends RunConfiguration {
    @Deprecated
    default void addConnection(@Nonnull final Connection<?, ?> connection) {}

    default Module getModule() {
        return null;
    }

    @Nonnull
    default List<Connection<?, ?>> getConnections() {
        return AzureProjectFacet.getAzureModule(this)
                .map(AzureModule::getDefaultProfile)
                .map(Profile::getConnections)
                .orElse(Collections.emptyList());
    }

    default DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask getLoadDotEnvBeforeRunTask() {
        return (DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask) this.getBeforeRunTasks().stream()
                .filter(task -> task instanceof DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask).findAny().orElse(null);
    }

    default boolean isConnectionEnabled() {
        return Objects.nonNull(getLoadDotEnvBeforeRunTask());
    }
}
