package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.ide.guidance.config.CourseConfig;
import com.microsoft.azure.toolkit.ide.guidance.view.GuidanceView;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class GuidanceViewManager {

    public static final String TOOL_WINDOW_ID = "Getting Started with Azure";

    private static final GuidanceViewManager instance = new GuidanceViewManager();

    public static GuidanceViewManager getInstance() {
        return instance;
    }

    @AzureOperation(name = "guidance.open_course.course", params = {"courseConfig.getTitle()"}, type = AzureOperation.Type.ACTION)
    public void openCourseView(@Nonnull final Project project, @Nonnull final CourseConfig courseConfig) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GuidanceViewManager.TOOL_WINDOW_ID);
        AzureTaskManager.getInstance().runLater(() -> {
            assert toolWindow != null;
            toolWindow.activate(() -> {
                toolWindow.setAvailable(true);
                toolWindow.show();
                final GuidanceView guidanceView = GuidanceViewFactory.getGuidanceView(project);
                if (Objects.nonNull(guidanceView)) {
                    final Course course = new Course(courseConfig, project);
                    guidanceView.showCourseView(course);
                }
            });
        });
    }

    public void showCoursesView(@Nonnull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GuidanceViewManager.TOOL_WINDOW_ID);
        AzureTaskManager.getInstance().runLater(() -> {
            assert toolWindow != null;
            toolWindow.activate(() -> {
                toolWindow.setAvailable(true);
                toolWindow.show();
                final GuidanceView guidanceView = GuidanceViewFactory.getGuidanceView(project);
                if (Objects.nonNull(guidanceView)) {
                    guidanceView.showCoursesView();
                }
            });
        });
    }

    @AzureOperation(name = "guidance.close_course", type = AzureOperation.Type.ACTION)
    public void closeGuidanceToolWindow(@Nonnull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GuidanceViewManager.TOOL_WINDOW_ID);
        AzureTaskManager.getInstance().runLater(() -> {
            final GuidanceView guidanceView = GuidanceViewFactory.getGuidanceView(project);
            if (Objects.nonNull(guidanceView)) {
                guidanceView.showCoursesView();
            }
            assert toolWindow != null;
            toolWindow.hide();
        });
    }

    public static class GuidanceViewFactory implements ToolWindowFactory {
        private static final Map<Project, GuidanceView> guidanceViewMap = new ConcurrentHashMap<>();

        @Override
        public boolean shouldBeAvailable(@NotNull Project project) {
            return false;
        }

        @Override
        public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
            final GuidanceView guidanceView = new GuidanceView(project);
            guidanceViewMap.put(project, guidanceView);
            final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            final JBScrollPane view = new JBScrollPane(guidanceView, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            final Content content = contentFactory.createContent(view, "", false);
            toolWindow.getContentManager().addContent(content);
        }

        public static GuidanceView getGuidanceView(@Nonnull final Project project) {
            return guidanceViewMap.get(project);
        }
    }
}
