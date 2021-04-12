/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.util.PluginUtil;

public class AzurePopupGroup extends DefaultActionGroup implements DumbAware {

    public void update(AnActionEvent e) {
        Module module = LangDataKeys.MODULE.getData(e.getDataContext());
        if (module == null) {
            e.getPresentation().setEnabledAndVisible(false);
        } else {
            VirtualFile selectedFile = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
            e.getPresentation().setEnabledAndVisible(PluginUtil.isModuleRoot(selectedFile, module)
                    && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE)));
        }
    }
}
