/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.dependency;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

public class AddDependencyContextAction extends AddDependencyAction {
    public void update(@NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        final Module module = event.getData(LangDataKeys.MODULE);

        presentation.setEnabledAndVisible(isEditingMavenPomXml(module, event));
    }
}
