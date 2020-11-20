/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
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
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.ui.components.DefaultDialogWrapper;
import com.microsoft.intellij.ui.libraries.ApplicationInsightsPanel;
import com.microsoft.intellij.util.MavenUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApplicationInsightsAction extends AzureAnAction {
    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {
        final Module module = event.getData(LangDataKeys.MODULE);
        DefaultDialogWrapper dialog = new DefaultDialogWrapper(module.getProject(), new ApplicationInsightsPanel(module));
        dialog.show();
        return true;
    }

    @Override
    public void update(AnActionEvent event) {
        final Module module = event.getData(LangDataKeys.MODULE);
        boolean isMavenOrNull = (module == null || MavenUtils.isMavenProject(module.getProject()));
        event.getPresentation().setEnabledAndVisible(!isMavenOrNull && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
    }
}
