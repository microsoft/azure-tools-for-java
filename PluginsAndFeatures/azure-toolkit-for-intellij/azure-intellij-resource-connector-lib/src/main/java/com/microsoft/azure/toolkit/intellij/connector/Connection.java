/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * the <b>{@code resource connection}</b>
 *
 * @param <R> type of the resource consumed by {@link C}
 * @param <C> type of the consumer consuming {@link R},
 *            it can only be {@link ModuleResource} for now({@code v3.52.0})
 * @since 3.52.0
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Connection<R, C> {
    @Nonnull
    @EqualsAndHashCode.Include
    protected final Resource<R> resource;
    @Nonnull
    @EqualsAndHashCode.Include
    protected final Resource<C> consumer;
    @Nonnull
    @EqualsAndHashCode.Include
    protected final ConnectionDefinition<R, C> definition;

    @Nonnull
    public String getDefName() {
        return this.getDefinition().getName();
    }

    /**
     * is this connection applicable for the specified {@code configuration}.<br>
     * - the {@code Connect Azure Resource} before run task will take effect if
     * applicable: the {@link #prepareBeforeRun} & {@link #updateJavaParametersAtRun}
     * will be called.
     *
     * @return true if this connection should intervene the specified {@code configuration}.
     */
    public boolean isApplicableFor(@NotNull RunConfiguration configuration) {
        return false;
    }

    /**
     * do some preparation in the {@code Connect Azure Resource} before run task
     * of the {@code configuration}<br>
     */
    public boolean prepareBeforeRun(@NotNull RunConfiguration configuration, DataContext dataContext) {
        return false;
    }

    /**
     * update java parameters exactly before start the {@code configuration}
     */
    public void updateJavaParametersAtRun(RunConfiguration configuration, @NotNull JavaParameters parameters) {
    }

    public void write(Element connectionEle) {
        this.getDefinition().write(connectionEle, this);
    }
}
