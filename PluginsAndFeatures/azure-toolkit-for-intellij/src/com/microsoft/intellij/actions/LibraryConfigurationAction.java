/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.roots.ModuleRootManager;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.ui.libraries.AzureLibrary;
import com.microsoft.intellij.ui.libraries.LibrariesConfigurationDialog;
import com.microsoft.intellij.util.MavenUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LibraryConfigurationAction extends AzureAnAction {

    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {
        final Module module = event.getData(LangDataKeys.MODULE);
        List<AzureLibrary> currentLibs = new ArrayList<AzureLibrary>();
        for (AzureLibrary azureLibrary : AzureLibrary.LIBRARIES) {
            if (ModuleRootManager.getInstance(module).getModifiableModel().getModuleLibraryTable().getLibraryByName(azureLibrary.getName()) != null) {
                currentLibs.add(azureLibrary);
            }
        }
        LibrariesConfigurationDialog configurationDialog = new LibrariesConfigurationDialog(module, currentLibs);
        configurationDialog.show();
        return true;
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.SYSTEM;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.LIB_CONFIGURATION;
    }

    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        boolean isMavenOrNull = (module == null || MavenUtils.isMavenProject(module.getProject()));
        event.getPresentation().setEnabledAndVisible(!isMavenOrNull && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}
