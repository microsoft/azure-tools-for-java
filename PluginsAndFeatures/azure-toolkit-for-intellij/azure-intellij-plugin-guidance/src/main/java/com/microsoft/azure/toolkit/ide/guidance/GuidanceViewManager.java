package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.ide.guidance.config.SequenceConfig;
import com.microsoft.azure.toolkit.ide.guidance.view.GuidanceView;
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

    public void showGuidance(@Nonnull final Project project, @Nonnull final SequenceConfig sequenceConfig) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GuidanceViewManager.TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        AzureTaskManager.getInstance().runLater(() -> {
            toolWindow.show();
            final GuidanceView guidanceView = (GuidanceView) Arrays.stream(toolWindow.getComponent().getComponents())
                    .filter(component -> component instanceof GuidanceView).findFirst().orElse(null);
            if (guidanceView != null) {
                guidanceView.showProcess(GuidanceViewManager.createProcess(sequenceConfig, project));
            }
        });
    }

    public void showGuidanceWelcome(@Nonnull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GuidanceViewManager.TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
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

    public static Guidance createProcess(@Nonnull final SequenceConfig config, @Nonnull Project project) {
        final Guidance guidance = new Guidance(config, project);
        AzureTaskManager.getInstance().runOnPooledThread(guidance::init);
        return guidance;
    }
}
