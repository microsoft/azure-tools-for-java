/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guidance.view.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Status;
import com.microsoft.azure.toolkit.ide.guidance.Step;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_NONE;

public class SummaryPanel extends JPanel {
    // JBUI.CurrentTheme.Tree.Hover.background(true)
    // com.intellij.notification.impl.NotificationComponent.NEW_COLOR
    private static final JBColor BACKGROUND_COLOR = JBColor.namedColor("NotificationsToolwindow.newNotification.background", new JBColor(15134455, 4540746));
    private final Phase phase;
    private JPanel contentPanel;
    private JLabel statusIcon;
    private JLabel titleLabel;
    private JPanel detailsPanel;
    private JTextPane descPanel;
    private boolean focused;

    private JButton defaultButton = null;
    private AzureEventBus.EventListener listener = null;

    public SummaryPanel(@Nonnull Phase phase) {
        super();
        this.phase = phase;
        $$$setupUI$$$();
        init();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (this.focused) {
            Optional.ofNullable(defaultButton).ifPresent(button -> Optional.ofNullable(getRootPane()).ifPresent(pane -> pane.setDefaultButton(button)));
        }
    }

    private void init() {
        final GridLayoutManager layout = new GridLayoutManager(1, 1);
        final Insets margin = JBUI.insets(8, 8, 10, 8);
        layout.setMargin(margin);
        this.setLayout(layout);
        this.setVisible(false); // invisible in default
        this.add(this.contentPanel, new GridConstraints(0, 0, 1, 1, 0, GridConstraints.ALIGN_FILL, 3, 3, null, null, null, 0));
        //https://stackoverflow.com/questions/7115065/jlabel-vertical-alignment-not-working-as-expected
        this.titleLabel.setBorder(BorderFactory.createEmptyBorder(-2 /*top*/, 0, 0, 0));
        this.titleLabel.setText(this.phase.getTitle());
        this.descPanel.setBorder(null);
        this.descPanel.setText(this.phase.getDescription());
        this.descPanel.setVisible(StringUtils.isNotBlank(this.phase.getDescription()));
        this.phase.addStatusListener(this::updateStatus);
    }

    private void updateStatus(Status status) {
        this.statusIcon.setIcon(AllIcons.General.BalloonInformation);
        this.focused = status == Status.READY || status == Status.RUNNING || status == Status.FAILED || status == Status.SUCCEED || status == Status.PARTIAL_SUCCEED;
        if (status == Status.SUCCEED) {
            initDetailsPanel();
        }
        this.setVisible(this.focused);
        final Color bgColor = this.focused ? BACKGROUND_COLOR : JBUI.CurrentTheme.ToolWindow.background();
        PhasePanel.doForOffsprings(this.contentPanel, c -> c.setBackground(bgColor));
        if (this.focused) {
            Optional.ofNullable(defaultButton).ifPresent(button -> Optional.ofNullable(getRootPane()).ifPresent(pane -> pane.setDefaultButton(button)));
        }
    }

    protected void paintComponent(@Nonnull Graphics g) {
        super.paintComponent(g);
        if (this.focused) {
            final Graphics2D graphics = (Graphics2D) g;
            final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
            g.setColor(BACKGROUND_COLOR);
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
            config.restore();
        }
    }

    private void initDetailsPanel() {
        final List<Step> steps = phase.getSteps().stream().filter(step -> step.isReady()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(steps)) {
            // skip render steps panel when empty
            this.detailsPanel.setVisible(false);
            return;
        }
        this.detailsPanel.setLayout(new GridLayoutManager(1, steps.size() + 1));
        for (int i = 0; i < steps.size(); i++) {
            final Step step = steps.get(i);
            final Action<Void> executeAction = new Action<Void>(Action.Id.of("user/guidance.execute_summary_step.step"))
                    .withIdParam(step.getTitle())
                    .withAuthRequired(false)
                    .withHandler(ignore -> executeStep(step));
            final AzureActionButton<Void> button = new AzureActionButton<>();
            button.setText(step.getTitle());
            button.setAction(executeAction);
            if (i == 0) {
                SummaryPanel.this.defaultButton = button;
            }
            final GridConstraints gridConstraints = new GridConstraints(0, i, 1, 1, 0, FILL_NONE, 3, 3, null, null, null, 0);
            this.detailsPanel.add(button, gridConstraints);
        }
        detailsPanel.add(new Spacer(), new GridConstraints(0, steps.size(), 1, 1, 0, FILL_HORIZONTAL, 7, 3, null, null, null, 0));
    }

    private void executeStep(final Step step) {
        OperationContext.current().setTelemetryProperty("step", step.getTitle());
        OperationContext.current().setTelemetryProperty("phase", phase.getTitle());
        OperationContext.current().setTelemetryProperty("course", phase.getCourse().getTitle());
        step.execute();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

}
