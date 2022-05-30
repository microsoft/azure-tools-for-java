package com.microsoft.azure.toolkit.ide.guideline.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.guideline.GuidanceViewManager;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.AzureAnAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShowGettingStartAction extends AzureAnAction {
    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        GuidanceViewManager.getInstance().showGuidanceWelcome(anActionEvent.getProject());
        return false;
    }
}
