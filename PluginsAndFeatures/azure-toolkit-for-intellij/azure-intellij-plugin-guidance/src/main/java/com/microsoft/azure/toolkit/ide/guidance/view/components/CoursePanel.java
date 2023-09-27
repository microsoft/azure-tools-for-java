package com.microsoft.azure.toolkit.ide.guidance.view.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.ide.guidance.action.ShowGettingStartAction;
import com.microsoft.azure.toolkit.ide.guidance.config.CourseConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.RoundedPanel;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class CoursePanel {
    private final CourseConfig course;
    @Getter
    private JPanel rootPanel;
    private JLabel lblTitle;
    private JTextPane areaDescription;
    private AzureActionButton<Void> startButton;
    private JPanel tagsPanel;

    private final Project project;
    private boolean isStartedActionTriggered;
    private final boolean showNewUIFlag;
    public static final JBColor NOTIFICATION_BACKGROUND_COLOR =
            JBColor.namedColor("StatusBar.hoverBackground", new JBColor(15595004, 4606541));

    public CoursePanel(@Nonnull final CourseConfig course, @Nonnull final Project project) {
        super();
        // this.showNewUIFlag = Boolean.parseBoolean(Optional.ofNullable(ExperimentationClient.getExperimentationService())
        //         .map(service -> service.getFeatureVariable(ExperimentationClient.FeatureFlag.GETTING_STARTED_UI.getFlagName())).orElse("false"));
        this.showNewUIFlag = true;
        this.course = course;
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.lblTitle.setFont(JBFont.h4());
        // render course
        // this.lblIcon.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        this.lblTitle.setText(course.getTitle());
        this.lblTitle.setPreferredSize(new Dimension(-1, startButton.getPreferredSize().height));
        final Icon icon = Optional.ofNullable(course.getIcon()).map(IntelliJAzureIcons::getIcon).orElse(null);
        if (icon != null) {
            this.lblTitle.setIcon(icon);
        }
        this.startButton.setVisible(false);
        final Action<Void> startAction = new Action<Void>(Action.Id.of("user/guidance.open_course.course"))
                .withAuthRequired(false)
                .withIdParam(ignore -> this.course.getTitle())
                .withHandler(ignore -> openGuidance());
        this.startButton.setAction(startAction);
        this.areaDescription.setFont(JBFont.medium());
        this.areaDescription.setText(course.getDescription());
        this.areaDescription.setForeground(UIUtil.getLabelInfoForeground());
        if (showNewUIFlag) {
            this.course.getTags().forEach(tag -> this.tagsPanel.add(decorateTagLabel(tag)));
            this.startButton.setText("Try It");
            this.areaDescription.setForeground(null);
        }
    }

    public void toggleSelectedStatus(final boolean isSelected) {
        if (Objects.equals(isSelected, startButton.isVisible())) {
            return;
        }
        this.startButton.setVisible(isSelected);
        this.setBackgroundColor(this.rootPanel, isSelected ? NOTIFICATION_BACKGROUND_COLOR : UIUtil.getLabelBackground());
        if (isSelected && showNewUIFlag) {
            Optional.ofNullable(this.getRootPanel().getRootPane()).ifPresent(pane -> pane.setDefaultButton(this.startButton));
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    public void addMouseListener(@Nonnull final MouseListener coursePanelListener) {
        this.rootPanel.addMouseListener(coursePanelListener);
    }

    public void openGuidance() {
        if (!isStartedActionTriggered) {
            isStartedActionTriggered = true;
            AzureStoreManager.getInstance().getIdeStore().setProperty(ShowGettingStartAction.GUIDANCE, ShowGettingStartAction.IS_ACTION_TRIGGERED, String.valueOf(true));
        }
        OperationContext.current().setTelemetryProperty("course", this.course.getTitle());
        GuidanceViewManager.getInstance().openCourseView(project, course);
    }

    private JLabel decorateTagLabel(String tag) {
        final JLabel label = new JLabel(tag);
        final Border borderLine = new TagLineBorder(new JBColor(12895428, 6185056), 2);
        final Border margin = JBUI.Borders.empty(0, 6);
        label.setBorder(new CompoundBorder(borderLine, margin));
        label.setFont(JBFont.regular().lessOn(2));
        return label;
    }

    private void setBackgroundColor(@Nonnull final JPanel c, @Nonnull final Color color) {
        c.setBackground(color);
        Arrays.stream(c.getComponents()).filter(component -> component instanceof JPanel).forEach(child -> setBackgroundColor((JPanel) child, color));
        Arrays.stream(c.getComponents()).filter(component -> component instanceof JTextPane || component instanceof JButton).forEach(child -> child.setBackground(color));
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.rootPanel = new RoundedPanel(5);
        this.tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        this.tagsPanel.setBorder(JBUI.Borders.emptyLeft(-8));
    }
}
