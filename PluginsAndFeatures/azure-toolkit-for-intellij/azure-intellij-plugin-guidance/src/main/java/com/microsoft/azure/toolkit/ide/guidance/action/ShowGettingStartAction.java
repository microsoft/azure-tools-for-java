package com.microsoft.azure.toolkit.ide.guidance.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceConfigManager;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.GET_START;
import static com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.GET_START_NEW;

public class ShowGettingStartAction extends AnAction implements DumbAware {
    private static boolean isActionTriggered = false;

    @Override
    @AzureOperation(name = "user/guidance.show_courses_view")
    public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
        if (anActionEvent.getProject() != null) {
            OperationContext.action().setTelemetryProperty("FromPlace", anActionEvent.getPlace());
            OperationContext.action().setTelemetryProperty("ShowBlueIcon", String.valueOf(!isActionTriggered));
            if (!isActionTriggered) {
                isActionTriggered = true;
                Azure.az().config().set(GuidanceConfigManager.IS_ACTION_TRIGGERED, true);
            }
            GuidanceViewManager.getInstance().showCoursesView(anActionEvent.getProject());
        }
    }

    @Override
    public void update(AnActionEvent e) {
        if (!isActionTriggered) {
            isActionTriggered = Azure.az().config().get(GuidanceConfigManager.IS_ACTION_TRIGGERED, false);
        }
        e.getPresentation().setIcon(IntelliJAzureIcons.getIcon(isActionTriggered ? GET_START : GET_START_NEW));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
