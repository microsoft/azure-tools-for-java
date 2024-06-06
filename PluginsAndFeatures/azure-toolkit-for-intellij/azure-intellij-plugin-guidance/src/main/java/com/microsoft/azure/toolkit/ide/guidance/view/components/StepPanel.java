package com.microsoft.azure.toolkit.ide.guidance.view.components;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.guidance.Status;
import com.microsoft.azure.toolkit.ide.guidance.Step;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.guidance.view.components.PhasePanel.OPEN_IN_BROWSER;

public class StepPanel extends JPanel {
    private JPanel contentPanel;
    private JLabel statusIcon;
    private JTextPane titleLabel;
    private JTextPane descPanel;
    private JTextArea outputPanel;
    private com.intellij.ui.components.ActionLink actionButton;

    private final Step step;

    public StepPanel(@Nonnull final Step step) {
        super();
        this.step = step;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.setLayout(new GridLayoutManager(1, 1));
        this.add(this.contentPanel, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0));
        this.step.addStatusListener(this::updateStatus);
        this.titleLabel.setBorder(null);
        this.titleLabel.setContentType("text/html");
        this.titleLabel.setEditorKit(new UIUtil.JBWordWrapHtmlEditorKit());
        this.titleLabel.addHyperlinkListener(OPEN_IN_BROWSER);

        this.actionButton.setText("Run");
        this.actionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.actionButton.addActionListener(e -> execute());
        this.descPanel.setBorder(null);
        this.descPanel.setVisible(StringUtils.isNotBlank(this.step.getDescription()));
        this.renderDescription();
        this.initOutputPanel();
        this.updateStatus(this.step.getStatus());
        this.step.getContext().addContextListener(ignore -> this.renderDescription());
    }

    @AzureOperation(name = "user/guidance.execute_step.step", params = {"this.step.getTitle()"})
    private void execute() {
        OperationContext.current().setTelemetryProperty("step", this.step.getTitle());
        OperationContext.current().setTelemetryProperty("phase", this.step.getPhase().getTitle());
        OperationContext.current().setTelemetryProperty("course", this.step.getPhase().getCourse().getTitle());
        this.step.execute();
    }

    private void renderDescription() {
        titleLabel.setText(step.getRenderedTitle());
        descPanel.setText(step.getRenderedDescription());
    }

    private void initOutputPanel() {
        final IAzureMessager messager = new ConsoleTextMessager();
        this.step.setOutput(messager);
        this.outputPanel.setVisible(false);
        final CompoundBorder border = BorderFactory.createCompoundBorder(this.outputPanel.getBorder(), BorderFactory.createEmptyBorder(2, 4, 4, 4));
        this.outputPanel.setBorder(border);
        this.outputPanel.setBackground(JBUI.CurrentTheme.EditorTabs.background());
    }

    class ConsoleTextMessager implements IAzureMessager {
        @Override
        public boolean show(IAzureMessage message) {
            Optional.ofNullable(StepPanel.this.step.getPhase().getOutput()).ifPresent(messager -> messager.show(message)); // Also write to step output
            return true;
        }
    }

    private void updateStatus(Status status) {
        this.updateStatusIcon(status);
        this.actionButton.setVisible(status != Status.SUCCEED && status != Status.PARTIAL_SUCCEED);
        this.actionButton.setEnabled(status == Status.READY || status == Status.FAILED);
        if (status == Status.FAILED) {
            this.actionButton.setText("Retry");
        }
    }

    void updateStatusIcon(final Status status) {
        if (status == Status.RUNNING || status == Status.SUCCEED || status == Status.PARTIAL_SUCCEED || status == Status.FAILED) {
            final Icon icon = IconUtil.scale(PhasePanel.getStatusIcon(status), this.statusIcon, 0.875f);
            this.statusIcon.setIcon(icon);
            this.statusIcon.setText("");
        } else {
            this.statusIcon.setIcon(null);
            this.statusIcon.setFont(JBUI.Fonts.create("JetBrains Mono", 12).asBold());
            final int index = this.step.getPhase().getSteps().indexOf(this.step);
            this.statusIcon.setText(String.valueOf((char) ('a' + index)));
            this.statusIcon.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        }
    }

    void $$$setupUI$$$() {
    }
}
