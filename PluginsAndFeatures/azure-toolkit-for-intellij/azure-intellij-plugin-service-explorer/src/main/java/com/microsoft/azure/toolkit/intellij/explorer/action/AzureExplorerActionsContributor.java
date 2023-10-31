package com.microsoft.azure.toolkit.intellij.explorer.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Optional;
import java.util.function.BiConsumer;

public class AzureExplorerActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<Object, AnActionEvent> openAzureExplorer = (ignore, e) -> Optional.ofNullable(e).map(AnActionEvent::getProject)
            .map(ToolWindowManager::getInstance)
            .map(m -> m.getToolWindow(AzureExplorer.TOOLWINDOW_ID))
            .ifPresent(w -> AzureTaskManager.getInstance().runLater(() -> w.show(null)));
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER, (i, e) -> true, openAzureExplorer);
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    }
}
