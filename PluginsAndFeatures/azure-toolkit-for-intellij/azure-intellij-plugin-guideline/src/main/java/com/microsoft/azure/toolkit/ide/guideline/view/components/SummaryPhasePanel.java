package com.microsoft.azure.toolkit.ide.guideline.view.components;

import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.Phase;
import com.microsoft.azure.toolkit.ide.guideline.Status;
import com.microsoft.azure.toolkit.ide.guideline.Step;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.codehaus.plexus.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class SummaryPhasePanel extends JPanel {
    private JPanel pnlContentHolder;
    private JTextPane paneDescription;
    private JPanel pnlTasks;
    private JPanel pnlContent;
    private JPanel pnlRoot;

    private HideableDecorator phaseDecorator;
    private Phase summaryPhase;

    public SummaryPhasePanel(Phase summaryPhase) {
        this.summaryPhase = summaryPhase;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.setLayout(new GridLayoutManager(1, 1));
        this.add(pnlRoot, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0));

        phaseDecorator = new HideableDecorator(pnlContentHolder, summaryPhase.getTitle(), false);
        phaseDecorator.setOn(summaryPhase.getStatus() == Status.READY);
        phaseDecorator.setContentComponent(pnlContent);

        paneDescription.setText(summaryPhase.getDescription());

        renderActions();

        summaryPhase.addStatusListener(phaseStatus -> update(phaseStatus));
    }

    private void update(Status phaseStatus) {
        if (phaseStatus == Status.READY) {
//            this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            this.setBackgroundColor(this.getPanel(), new Color(69, 73, 74));
            this.phaseDecorator.setOn(true);
        }
        if (phaseStatus == Status.SUCCEED) {
//            this.setBorder(null);
            this.setBackgroundColor(this.getPanel(), JBColor.background());
            this.phaseDecorator.setOn(false);
        }
    }

    private void setBackgroundColor(JPanel panel, Color color) {
        panel.setBackground(color);
        Arrays.stream(panel.getComponents()).filter(component -> component instanceof JPanel).forEach(child -> setBackgroundColor((JPanel) child, color));
        Arrays.stream(panel.getComponents()).filter(component -> component instanceof JTextPane || component instanceof JButton).forEach(child -> child.setBackground(color));
    }

    private void renderActions() {
        final List<Step> steps = summaryPhase.getSteps();
        pnlTasks.setLayout(new GridLayoutManager(steps.size(), 1));
        for (int i = 0; i < steps.size(); i++) {
            final Step step = steps.get(i);
            HyperlinkLabel hyperlinkLabel = new HyperlinkLabel();
            hyperlinkLabel.setHyperlinkText(StringUtils.capitalise(step.getTitle()));
            hyperlinkLabel.addHyperlinkListener(e -> runStep(step));
            final GridConstraints gridConstraints = new GridConstraints(i, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
            pnlTasks.add(hyperlinkLabel, gridConstraints);
        }
    }

    private void runStep(final Step step) {
        final Context context = summaryPhase.getProcess().getContext();
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(AzureString.format("Running action: %s", step.getTitle()), () -> {
            try {
                step.execute(context);
            } catch (Exception e) {
                AzureMessager.getMessager().error(e);
            }
        }));
    }

    public JPanel getPanel() {
        return this.pnlContentHolder;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
