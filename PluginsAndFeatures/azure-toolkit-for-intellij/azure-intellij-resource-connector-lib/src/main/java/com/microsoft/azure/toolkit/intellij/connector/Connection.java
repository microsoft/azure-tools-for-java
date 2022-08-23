/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    public static final String ENV_PREFIX = "%ENV_PREFIX%";
    private static final String SPRING_BOOT_CONFIGURATION = "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration";
    @Nonnull
    @EqualsAndHashCode.Include
    protected final Resource<R> resource;
    @Nonnull
    @EqualsAndHashCode.Include
    protected final Resource<C> consumer;
    @Nonnull
    @EqualsAndHashCode.Include
    protected final ConnectionDefinition<R, C> definition;
    @Setter
    @Getter(AccessLevel.NONE)
    private String envPrefix;
    private Map<String, String> env = new HashMap<>();

    /**
     * is this connection applicable for the specified {@code configuration}.<br>
     * - the {@code Connect Azure Resource} before run task will take effect if
     * applicable: the {@link #prepareBeforeRun} & {@link #updateJavaParametersAtRun}
     * will be called.
     *
     * @return true if this connection should intervene the specified {@code configuration}.
     */
    public boolean isApplicableFor(@Nonnull RunConfiguration configuration) {
        final boolean javaAppRunConfiguration = configuration instanceof ApplicationConfiguration;
        final boolean springbootAppRunConfiguration = StringUtils.equals(configuration.getClass().getName(), SPRING_BOOT_CONFIGURATION);
        final boolean azureWebAppRunConfiguration = configuration instanceof IWebAppRunConfiguration;
        if (javaAppRunConfiguration || azureWebAppRunConfiguration || springbootAppRunConfiguration) {
            final Module module = getTargetModule(configuration);
            return Objects.nonNull(module) && Objects.equals(module.getName(), this.consumer.getName());
        }
        return false;
    }

    public Map<String, String> getEnvironmentVariables(final Project project) {
        return this.resource.initEnv(project).entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().replaceAll(Connection.ENV_PREFIX, this.getEnvPrefix()), Map.Entry::getValue));
    }

    /**
     * do some preparation in the {@code Connect Azure Resource} before run task
     * of the {@code configuration}<br>
     */
    @AzureOperation(name = "connector.prepare_before_run", type = AzureOperation.Type.ACTION)
    public boolean prepareBeforeRun(@Nonnull RunConfiguration configuration, DataContext dataContext) {
        try {
            this.env = getEnvironmentVariables(configuration.getProject());
            if (configuration instanceof IConnectionAware) { // set envs for remote deploy
                ((IConnectionAware) configuration).setConnection(this);
            }
            return true;
        } catch (final Throwable e) {
            AzureMessager.getMessager().error(e);
            return false;
        }
    }

    /**
     * update java parameters exactly before start the {@code configuration}
     */
    public void updateJavaParametersAtRun(@Nonnull RunConfiguration configuration, @Nonnull JavaParameters parameters) {
        if (Objects.nonNull(this.env)) {
            for (final Map.Entry<String, String> entry : this.env.entrySet()) {
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
        }
        return null;
    }

    public String getEnvPrefix() {
        if (StringUtils.isBlank(this.envPrefix)) {
            return this.definition.getResourceDefinition().getDefaultEnvPrefix();
        }
        return this.envPrefix;
    }

    public void write(Element connectionEle) {
        this.getDefinition().write(connectionEle, this);
    }

    public boolean validate(Project project) {
        return this.getDefinition().validate(this, project);
    }
}
