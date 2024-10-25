/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static final Pattern SPRING_PROPERTY_VALUE_PATTERN = Pattern.compile("\\$\\{(.*)}");

    public static boolean hasEnvVars(final String value) {
        return value.matches(".*\\$\\{[^}]+}.*");
    }

    public static String extractVariableFromSpringProperties(final String origin) {
        final Matcher matcher = SPRING_PROPERTY_VALUE_PATTERN.matcher(origin);
        return matcher.matches() ? matcher.group(1) : origin;
    }

    @Nullable
    public static Connection<?, ?> getConnectionWithEnvironmentVariable(@Nullable final Module module,
                                                                        @Nonnull String variable) {
        final Profile defaultProfile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(defaultProfile) || !defaultProfile.isEnvFileValid()) {
            return null;
        }
        return defaultProfile.getConnections().stream()
            .filter(c -> defaultProfile.getGeneratedEnvironmentVariables(c).stream()
                .anyMatch(pair -> StringUtils.equalsIgnoreCase(pair.getKey(), variable)))
            .findAny().orElse(null);
    }

    @Nullable
    public static Connection<? extends AzResource, ?> getConnectionWithResource(@Nullable final Module module,
                                                                                @Nonnull AzResource resource) {
        final Profile defaultProfile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(defaultProfile)) {
            return null;
        }
        //noinspection unchecked
        return (Connection<? extends AzResource, ?>) defaultProfile.getConnections().stream()
            .filter(c -> Objects.equals(c.getResource().getData(), resource))
            .findAny().orElse(null);
    }

    private static boolean isConnectionVariable(String variable, Profile defaultProfile, Connection<?, ?> c) {
        return defaultProfile.getGeneratedEnvironmentVariables(c).stream()
            .anyMatch(pair -> StringUtils.equalsIgnoreCase(pair.getKey(), variable));
    }

    @SuppressWarnings("rawtypes")
    @AzureOperation(name = "internal/connector.create_connection_in_properties")
    public static void createAndInsert(@Nonnull Module module, @Nonnull Resource resource,
                                       @Nonnull InsertionContext context, @Nonnull ConnectionManager connectionManager,
                                       @Nonnull BiConsumer<Connection, InsertionContext> insertHandler,
                                       @Nullable Consumer<InsertionContext> cancelHandler) {
        final Project project = context.getProject();
        if (resource.canConnectSilently()) {
            final Connection<?, ?> c = createSilently(module, resource, connectionManager);
            WriteCommandAction.runWriteCommandAction(project, () -> insertHandler.accept(c, context));
        } else {
            AzureTaskManager.getInstance().runLater(() -> {
                final var dialog = new ConnectorDialog(project);
                dialog.setConsumer(new ModuleResource(module.getName()));
                dialog.setResource(resource);
                if (dialog.showAndGet()) {
                    final Connection<?, ?> c = dialog.getValue();
                    WriteCommandAction.runWriteCommandAction(project, () -> insertHandler.accept(c, context));
                } else {
                    WriteCommandAction.runWriteCommandAction(project, () -> Optional.ofNullable(cancelHandler).ifPresent(c -> c.accept(context)));
                }
            });
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @AzureOperation(name = "internal/connector.creating_connection_silently_in_properties")
    private static Connection<?, ?> createSilently(Module module, @Nonnull Resource resource, ConnectionManager connectionManager) {
        final Resource consumer = ModuleResource.Definition.IJ_MODULE.define(module.getName());
        final ConnectionDefinition<?, ?> connectionDefinition = ConnectionManager.getDefinitionOrDefault(resource.getDefinition(), consumer.getDefinition());
        final Connection<?, ?> connection = connectionDefinition.define(resource, consumer);
        if (resource.getDefinition().isEnvPrefixSupported()) {
            connection.setEnvPrefix(connectionManager.getNewPrefix(resource, consumer));
        }
        final AzureTaskManager taskManager = AzureTaskManager.getInstance();
        taskManager.runLater(() -> taskManager.write(() -> {
            final Profile profile = connectionManager.getProfile().getModule().initializeWithDefaultProfileIfNot();
            profile.createOrUpdateConnection(connection);
            profile.save();
        }));
        return connection;
    }

    @Nullable
    public static PsiElement getPropertyField(final String fullQualifiedFieldName, final PsiFile file) {
        if (StringUtils.isNotBlank(fullQualifiedFieldName)) {
            final String[] parts = fullQualifiedFieldName.split("#");
            final PsiClass psiClass = JavaPsiFacade.getInstance(file.getProject()).findClass(parts[0], file.getResolveScope());
            return Optional.ofNullable(psiClass).map(c -> c.findFieldByName(parts[1], true)).orElse(null);
        }
        return null;
    }

    public static <T> List<T> getConnectedResources(@Nonnull Module module, @Nonnull ResourceDefinition<T> definition) {
        return checkCancelled(() -> AzureModule.from(module).getConnectedResources(definition));
    }

    public static <T> List<Connection<T, ?>> getConnections(@Nonnull Module module, @Nonnull ResourceDefinition<T> definition) {
        return checkCancelled(() -> AzureModule.from(module).getConnections(definition));
    }

    public static <T> List<T> checkCancelled(Supplier<List<T>> supplier) {
        final CompletableFuture<List<T>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (final Exception e) {
                return Collections.emptyList();
            }
        });
        while (!future.isDone()) {
            try {
                ProgressManager.checkCanceled();
                Thread.sleep(200);
            } catch (final InterruptedException e) {
                return Collections.emptyList();
            }
        }
        try {
            return future.get();
        } catch (final Exception e) {
            return Collections.emptyList();
        }
    }
}
