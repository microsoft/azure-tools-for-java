/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static final Pattern SPRING_PROPERTY_VALUE_PATTERN = Pattern.compile("\\$\\{(.*)}");

    public static String extractVariableFromSpringProperties(final String origin) {
        final Matcher matcher = SPRING_PROPERTY_VALUE_PATTERN.matcher(origin);
        return matcher.matches() ? matcher.group(1) : origin;
    }

    public static <T> List<T> getConnectedResources(@Nonnull final Module module, @Nonnull final ResourceDefinition<T> definition) {
        return Optional.of(module).map(AzureModule::from)
                .map(AzureModule::getDefaultProfile).map(Profile::getConnectionManager).stream()
                .flatMap(m -> m.getConnections().stream())
                .filter(c -> Objects.equals(c.getDefinition().getResourceDefinition(), definition))
                .map(Connection::getResource)
                .filter(Resource::isValidResource)
                .map(Resource::getData)
                .map(resource -> (T) resource)
                .toList();
    }

    @Nullable
    public static Connection<? extends AzResource, ?> getConnectionWithEnvironmentVariable(@Nullable final Module module,
                                                                                 @Nonnull String variable) {
        final Profile defaultProfile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(defaultProfile)) {
            return null;
        }
        return (Connection<? extends AzResource, ?>) defaultProfile.getConnections().stream()
                .filter(c -> isConnectionVariable(variable, defaultProfile, c))
                .findAny().orElse(null);
    }

    @Nullable
    public static Connection<? extends AzResource, ?> getConnectionWithResource(@Nullable final Module module,
                                                                                           @Nonnull AzResource resource) {
        final Profile defaultProfile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(defaultProfile)) {
            return null;
        }
        return (Connection<? extends AzResource, ?>) defaultProfile.getConnections().stream()
                .filter(c -> Objects.equals(c.getResource().getData(), resource))
                .findAny().orElse(null);
    }

    private static boolean isConnectionVariable(String variable, Profile defaultProfile, Connection<?, ?> c) {
        return defaultProfile.getGeneratedEnvironmentVariables(c).stream()
                .anyMatch(pair -> StringUtils.equalsIgnoreCase(pair.getKey(), variable));
    }
}
