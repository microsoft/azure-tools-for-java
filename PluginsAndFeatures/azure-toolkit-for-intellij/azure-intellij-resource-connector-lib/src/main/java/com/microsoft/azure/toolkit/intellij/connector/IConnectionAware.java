package com.microsoft.azure.toolkit.intellij.connector;

import javax.annotation.Nonnull;

public interface IConnectionAware {
    void setConnection(@Nonnull final Connection<?, ?> connection);
}
