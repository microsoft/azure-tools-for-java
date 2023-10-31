package com.microsoft.azure.toolkit.ide.guidance.view;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.AnActionLink;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBFont;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceConfigManager;
import com.microsoft.azure.toolkit.ide.guidance.config.CourseConfig;
import com.microsoft.azure.toolkit.ide.guidance.view.components.CoursePanel;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.action.ViewToolingDocumentAction;
import com.microsoft.azure.toolkit.intellij.common.action.WhatsNewAction;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoursesView {
    private JPanel pnlRoot;
    private JPanel pnlCourses;
    private JLabel lblTitle;
    private JPanel pnlLoading;
    private JLabel lblLoading;
    private ActionLink toolkitDocLink;
    private ActionLink newFeatureLink;
    private JPanel actionLinkPanel;

    private final Project project;

    private final List<CoursePanel> coursePanels = new ArrayList<>();

    public CoursesView(@Nonnull Project project) {
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.lblTitle.setFont(JBFont.h2().asBold());
        this.lblLoading.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.REFRESH_ICON));
        // final String showNewUiFlag = Optional.ofNullable(ExperimentationClient.getExperimentationService())
        //          .map(service -> service.getFeatureVariable(ExperimentationClient.FeatureFlag.GETTING_STARTED_UI.getFlagName())).orElse("false");
        this.actionLinkPanel.setVisible(true);
        AzureTaskManager.getInstance().runInBackground("Loading lesson", () -> GuidanceConfigManager.getInstance().loadCourses())
            .thenAccept(courses -> AzureTaskManager.getInstance().runLater(() -> this.setCourses(courses)));
    }

    private void setCourses(final List<CourseConfig> courseConfigs) {
        this.coursePanels.clear();
        this.lblLoading.setVisible(true);
        if (CollectionUtils.isEmpty(courseConfigs)) {
            return;
        }
        this.pnlCourses.setLayout(new GridLayoutManager(courseConfigs.size(), 1));
        for (int i = 0; i < courseConfigs.size(); i++) {
            final CoursePanel coursePanel = new CoursePanel(courseConfigs.get(i), this.project);
            coursePanel.getRootPanel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(coursePanel.getRootPanel(), new CoursePanelListener(coursePanel));
            this.coursePanels.add(coursePanel);
            this.pnlCourses.add(coursePanel.getRootPanel(),
                    new GridConstraints(i, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0));
        }
        this.lblLoading.setVisible(false);
        this.coursePanels.get(0).toggleSelectedStatus(true);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    public void setVisible(boolean visible) {
        this.pnlRoot.setVisible(visible);
    }

    private void cleanUpSelection() {
        coursePanels.forEach(panel -> panel.toggleSelectedStatus(false));
    }

    private void addMouseListener(@Nonnull final JComponent component, @Nonnull MouseListener mouseListener) {
        component.addMouseListener(mouseListener);
        Arrays.stream(component.getComponents()).forEach(child -> {
            if (child instanceof JComponent) {
                addMouseListener((JComponent) child, mouseListener);
            }
        });
    }

    private void createUIComponents() {
        this.toolkitDocLink = new AnActionLink("Documentation", ActionManager.getInstance().getAction(ViewToolingDocumentAction.ID));
        this.toolkitDocLink.setExternalLinkIcon();
        this.newFeatureLink = new AnActionLink("What's new", ActionManager.getInstance().getAction(WhatsNewAction.ID));
    }

    class CoursePanelListener extends MouseAdapter {
        private final CoursePanel panel;

        public CoursePanelListener(@Nonnull final CoursePanel panel) {
            this.panel = panel;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            cleanUpSelection();
            panel.toggleSelectedStatus(true);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            panel.openGuidance();
        }
    }
}
