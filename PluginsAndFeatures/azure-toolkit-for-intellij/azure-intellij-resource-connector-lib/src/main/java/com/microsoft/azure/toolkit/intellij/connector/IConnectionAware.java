package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.module.Module;

import javax.annotation.Nonnull;

public interface IConnectionAware {
    void setConnection(@Nonnull final Connection<?, ?> connection);

    default void addConnection(@Nonnull final Connection<?, ?> connection) {
        this.setConnection(connection);
    }

    default Module getModule() {
        return null;
    }
}
