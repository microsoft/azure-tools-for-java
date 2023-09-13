/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet;

import com.intellij.ProjectTopics;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Slf4j
public class AddFacetStartupActivity implements StartupActivity {

    @Nullable
    @Override
    @AzureOperation(name = "platform/connector.add_default_facet")
    public void runActivity(@Nonnull Project project) {
        if (project.isDisposed()) {
            return;
        }
        final ModuleManager moduleManager = ModuleManager.getInstance(project);
        for (final Module module : moduleManager.getModules()) {
            addFacetWhenNecessary(module);
        }
        project.getMessageBus().connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
            @Override
            public void rootsChanged(@NotNull ModuleRootEvent event) {
                final ModuleManager moduleManager = ModuleManager.getInstance(event.getProject());
                for (final Module module : moduleManager.getModules()) {
                    addFacetWhenNecessary(module);
                }
            }
        });
    }

    private static void addFacetWhenNecessary(Module module) {
        final AzureModule azureModule = AzureModule.from(module);
        if (azureModule.neverHasAzureFacet() && !azureModule.hasAzureFacet()) {
            if (azureModule.isInitialized() || azureModule.hasAzureDependencies()) {
                final AzureTaskManager tm = AzureTaskManager.getInstance();
                tm.runLater(() -> tm.write(() -> AzureFacet.addTo(module)));
            }
        }
    }
}
