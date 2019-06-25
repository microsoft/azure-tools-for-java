/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.roots.ModuleRootManager;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.intellij.ui.libraries.AzureLibrary;
import com.microsoft.intellij.ui.libraries.LibrariesConfigurationDialog;
import com.microsoft.intellij.util.MavenRunTaskUtil;

import java.util.ArrayList;
import java.util.List;

public class LibraryConfigurationAction extends AzureAnAction {

    public void onActionPerformed(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        List<AzureLibrary> currentLibs = new ArrayList<AzureLibrary>();
        for (AzureLibrary azureLibrary : AzureLibrary.LIBRARIES) {
            if (ModuleRootManager.getInstance(module).getModifiableModel().getModuleLibraryTable().getLibraryByName(azureLibrary.getName()) != null) {
                currentLibs.add(azureLibrary);
            }
        }
        LibrariesConfigurationDialog configurationDialog = new LibrariesConfigurationDialog(module, currentLibs);
        configurationDialog.show();
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
        boolean isMavenOrNull = (module == null || MavenRunTaskUtil.isMavenProject(module.getProject()));
        event.getPresentation().setEnabledAndVisible(!isMavenOrNull && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}
