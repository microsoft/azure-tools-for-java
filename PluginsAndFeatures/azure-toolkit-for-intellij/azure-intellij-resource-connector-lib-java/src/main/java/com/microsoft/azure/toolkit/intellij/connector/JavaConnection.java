/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * the <b>{@code resource connection}</b>
 *
 * @param <TResource> type of the resource consumed by {@link TConsumer}
 * @param <TConsumer> type of the consumer consuming {@link TResource},
 *            it can only be {@link ModuleResource} for now({@code v3.52.0})
 * @since 3.52.0
 */
public class JavaConnection<TResource, TConsumer> extends Connection<TResource, TConsumer> {

    private static final String SPRING_BOOT_CONFIGURATION = "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration";

    public JavaConnection(@Nonnull Resource<TResource> resource, @Nonnull Resource<TConsumer> consumer, @Nonnull ConnectionDefinition<TResource, TConsumer> definition) {
        super(resource, consumer, definition);
    }

    /**
     * is this connection applicable for the specified {@code configuration}.<br>
     * - the {@code Connect Azure Resource} before run task will take effect if
     * applicable: the {@link #prepareBeforeRun} & {@link #updateJavaParametersAtRun}
     * will be called.
     *
     * @return true if this connection should intervene the specified {@code configuration}.
     */
    public boolean isApplicableFor(@Nonnull RunConfiguration configuration) {
        final boolean isApplicable = super.isApplicableFor(configuration);
        final boolean appRunConfiguration = configuration instanceof ApplicationConfiguration;
        final boolean springbootAppRunConfiguration = StringUtils.equals(configuration.getClass().getName(), SPRING_BOOT_CONFIGURATION);

        if (isApplicable || appRunConfiguration || springbootAppRunConfiguration) {
            final Module module = getTargetModule(configuration);
            return Objects.nonNull(module) && Objects.equals(module.getName(), this.consumer.getName());
        }
        return false;
    }

    /**
     * update java parameters exactly before start the {@code configuration}
     */
    public void updateJavaParametersAtRun(@Nonnull RunConfiguration configuration, @Nonnull JavaParameters parameters) {
        if (this.isEnvironmentSet()) {
            for (final Map.Entry<String, String> entry : this.getEnvironmentEntries()) {
                parameters.addEnv(entry.getKey(), entry.getValue());
            }
        }
        if (this.resource.getDefinition() instanceof IJavaAgentSupported) {
            parameters.getVMParametersList()
                    .add(String.format("-javaagent:%s", ((IJavaAgentSupported) this.resource.getDefinition()).getJavaAgent().getAbsolutePath()));
        }
    }

    @Nullable
    private static Module getTargetModule(@Nonnull RunConfiguration configuration) {
        if (configuration instanceof ModuleBasedConfiguration) {
            return ((ModuleBasedConfiguration<?, ?>) configuration).getConfigurationModule().getModule();
        } else if (configuration instanceof IWebAppRunConfiguration) {
            return ((IWebAppRunConfiguration) configuration).getModule();
        } else if (configuration instanceof IConnectionAware) {
            return ((IConnectionAware) configuration).getModule();
        }
        return null;
    }

    public static class JavaConnectionProvider implements ConnectionProvider {
        @Override
        public <R, C> Connection<R, C> define(Resource<R> resource, Resource<C> consumer, ConnectionDefinition<R, C> definition) {
            return new JavaConnection<>(resource, consumer, definition);
        }
    }
}
