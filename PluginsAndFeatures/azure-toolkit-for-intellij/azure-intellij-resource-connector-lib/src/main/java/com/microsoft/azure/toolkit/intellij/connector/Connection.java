/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.identities.Identity;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
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

    @Getter
    @EqualsAndHashCode.Include
    @Nonnull
    private final String id;

    @Nonnull
    @Setter
    protected Resource<R> resource;

    @Nonnull
    @Setter
    protected Resource<C> consumer;

    @Nonnull
    @Setter
    protected ConnectionDefinition<R, C> definition;

    @Nullable
    @Setter
    protected AuthenticationType authenticationType;

    // todo: change authentication properties to different child class, rather than put them all in connection
    @Nullable
    @Setter
    protected Resource<Identity> userAssignedManagedIdentity;

    @Setter
    @Getter(AccessLevel.NONE)
    private String envPrefix;

    private Map<String, String> env = new HashMap<>();
    @Getter
    private Profile profile;

//    public String getId() {
//        return StringUtils.isBlank(this.id) ? this.getEnvPrefix() + "/" + resource.getId() : this.id;
//    }

    /**
     * is this connection applicable for the specified {@code configuration}.<br>
     * - the {@code Connect Azure Resource} before run task will take effect if
     * applicable: the {@link #prepareBeforeRun}
     * will be called.
     *
     * @return true if this connection should intervene the specified {@code configuration}.
     */
    public boolean isApplicableFor(@Nonnull RunConfiguration configuration) {
        return configuration instanceof IConnectionAware;
    }

    public Map<String, String> getEnvironmentVariables(final Project project) {
        final Map<String, String> result = this.resource.initEnv(project, this).entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().replaceAll(Connection.ENV_PREFIX, this.getEnvPrefix()), Map.Entry::getValue));
        if (this.getResource().getDefinition() instanceof FunctionSupported<R>) {
            result.putAll(((FunctionSupported<R>) this.getResource().getDefinition()).getPropertiesForFunction(this.getResource().getData(), this));
        }
        return result;
    }

    /**
     * do some preparation in the {@code Connect Azure Resource} before run task
     * of the {@code configuration}<br>
     */
    @AzureOperation(name = "internal/connector.prepare_before_run")
    public boolean prepareBeforeRun(@Nonnull RunConfiguration configuration, DataContext dataContext) {
        try {
            this.env = getEnvironmentVariables(configuration.getProject());
            return true;
        } catch (final Throwable e) {
            AzureMessager.getMessager().error(e);
            return false;
        }
    }

    public boolean isEnvironmentSet() {
        return Objects.nonNull(this.env);
    }

    public Set<Map.Entry<String, String>> getEnvironmentEntries() {
        return env.entrySet();
    }

    public String getEnvPrefix() {
        return resource.getDefinition().isEnvPrefixSupported() ?
            StringUtils.firstNonBlank(this.envPrefix, this.resource.getDefinition().getDefaultEnvPrefix()) : StringUtils.EMPTY;
    }

    public void write(Element connectionEle) {
        this.getDefinition().write(connectionEle, this);
    }

    public boolean validate(Project project) {
        return this.getDefinition().validate(this, project);
    }

    public boolean isValidConnection() {
        final boolean isResourceValid = this.getResource().isValidResource();
        final boolean isConsumerValid = this.getConsumer().isValidResource();
        return isResourceValid && isConsumerValid;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public List<Pair<String, String>> getGeneratedEnvironmentVariables() {
        return Optional.ofNullable(this.profile).map(p -> p.getGeneratedEnvironmentVariables(this)).orElse(Collections.emptyList());
    }

    public boolean isManagedIdentityConnection() {
        return this.authenticationType == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY ||
                this.authenticationType == AuthenticationType.SYSTEM_ASSIGNED_MANAGED_IDENTITY;
    }
}
