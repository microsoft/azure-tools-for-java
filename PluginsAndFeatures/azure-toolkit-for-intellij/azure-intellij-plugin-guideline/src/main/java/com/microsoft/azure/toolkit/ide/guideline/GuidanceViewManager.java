package com.microsoft.azure.toolkit.ide.guideline;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.ide.guideline.config.ProcessConfig;
import com.microsoft.azure.toolkit.ide.guideline.view.GuidanceView;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class GuidanceViewManager {

    public static final String TOOL_WINDOW_ID = "Azure Get Started";

    private static final GuidanceViewManager instance = new GuidanceViewManager();

    public static GuidanceViewManager getInstance() {
        return instance;
    }

    public void showGuidance(@Nonnull final Project project, @Nonnull final ProcessConfig processConfig) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GuidanceViewManager.TOOL_WINDOW_ID);
        AzureTaskManager.getInstance().runLater(() -> {
            toolWindow.show();
            final GuidanceView guidanceView = (GuidanceView) Arrays.stream(toolWindow.getComponent().getComponents())
                    .filter(component -> component instanceof GuidanceView).findFirst().orElse(null);
            if (guidanceView != null) {
                guidanceView.showProcess(GuidanceViewManager.createProcess(processConfig, project));
            }
        });
    }

    public void showGuidanceWelcome(@Nonnull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GuidanceViewManager.TOOL_WINDOW_ID);
        AzureTaskManager.getInstance().runLater(() -> {
            toolWindow.show();
            final GuidanceView guidanceView = (GuidanceView) Arrays.stream(toolWindow.getComponent().getComponents())
                    .filter(component -> component instanceof GuidanceView).findFirst().orElse(null);
            if (guidanceView != null) {
                guidanceView.showWelcomePage();
            }
        });
    }

    public static class GuidanceViewFactory implements ToolWindowFactory, DumbAware {
        @Override
        public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
            final GuidanceView view = new GuidanceView(project);
            final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            final Content content = contentFactory.createContent(view, "", false);
            toolWindow.getContentManager().addContent(content);
        }
    }

    public static Process createProcess(@Nonnull final ProcessConfig config, @Nonnull Project project) {
        final Process process = new Process(config, project);
        AzureTaskManager.getInstance().runOnPooledThread(() -> process.getPhases().get(0).prepareLaunch());
        return process;
    }
}
