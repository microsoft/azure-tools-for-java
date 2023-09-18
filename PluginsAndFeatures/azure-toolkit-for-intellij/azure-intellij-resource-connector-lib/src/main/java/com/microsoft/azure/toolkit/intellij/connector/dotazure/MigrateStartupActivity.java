/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.dotazure;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessage;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MigrateStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            AzureEventBus.once("account.logged_in.account", (a, b) -> migrate(project));
        } else {
            migrate(project);
        }
        return null;
    }

    @AzureOperation(name = "platform/connector.migrate_from_deprecated")
    private static void migrate(@Nonnull Project project) {
        if (project.isDisposed()) {
            return;
        }
        final ModuleManager moduleManager = ModuleManager.getInstance(project);
        final ConnectionManager manager = project.getService(ConnectionManager.class);
        final Map<String, List<Connection<?, ?>>> moduleConnections = manager
            .getConnections().stream()
            .collect(Collectors.groupingBy(c -> c.getConsumer().getName()));
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        moduleConnections.forEach((moduleName, connections) -> {
            Optional.ofNullable(moduleManager.findModuleByName(moduleName))
                .map(AzureModule::from).filter(m -> !m.isInitialized())
                .ifPresent(module -> tm.runLater(() -> tm.write(() -> {
                    final Profile profile = module.initializeWithDefaultProfileIfNot();
                    connections.forEach(c -> {
                        try {
                            manager.removeConnection(c.getResource().getId(), c.getConsumer().getId());
                            profile.addConnection(c);
                        } catch (final Exception e) {
                            final IntellijAzureMessage message = (IntellijAzureMessage) AzureMessager.getMessager().buildErrorMessage(e);
                            message.setProject(project).show();
                        }
                    });
                    profile.save();
                })));
        });
    }
}
