package com.microsoft.azure.toolkit.intellij.menu;

import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class AzureToolkitTopMenuGroup extends DefaultActionGroup{

    public AzureToolkitTopMenuGroup() {
        super();
        getTemplatePresentation().setText("Azure Toolkit");
    }
}
