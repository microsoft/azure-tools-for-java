package com.microsoft.azure.toolkit.ide.guideline.view.components;

import com.intellij.icons.AllIcons;
import com.intellij.ui.HideableDecorator;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.ide.guideline.Phase;
import com.microsoft.azure.toolkit.ide.guideline.Status;
import com.microsoft.azure.toolkit.ide.guideline.Step;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

public class PhasePanel extends JPanel {
    private JPanel pnlRoot;
    private JPanel pnlRootContent;
    private JLabel lblStatusIcon;
    private JLabel lblTitle;
    private JPanel pnlInputs;
    private JButton runButton;
    private JTextPane paneDescription;
    private JPanel pnlStepsHolder;
    private JPanel pnlSteps;

    private Phase phase;
    private HideableDecorator phaseDecorator;
    private HideableDecorator stepDecorator;

    public PhasePanel(@Nonnull Phase phase) {
        super();
        this.phase = phase;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.setLayout(new GridLayoutManager(1, 1));
        this.add(pnlRoot, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0));

        renderPhase();
    }

    private void renderPhase() {
        lblStatusIcon.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        lblTitle.setText(phase.getTitle());
        paneDescription.setText(phase.getDescription());

        phaseDecorator = new HideableDecorator(pnlRoot, phase.getTitle(), false);
        phaseDecorator.setOn(phase.getStatus() == Status.READY);
        phaseDecorator.setContentComponent(pnlRootContent);
        // Render Steps
        fillSteps();
        renderInputs();
        runButton.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Action.START));
        AzureEventBus.on("phase.run_phase", new AzureEventBus.EventListener(event -> {
            final Object source = event.getSource();
            if (source instanceof Phase && StringUtils.equals(((Phase) source).getId(), phase.getId())) {
                lblStatusIcon.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.REFRESH_ICON));
            }
        }));
        AzureEventBus.on("phase.run_phase_succeed", new AzureEventBus.EventListener(event -> {
            final Object source = event.getSource();
            if (source instanceof Phase && StringUtils.equals(((Phase) source).getId(), phase.getId())) {
                lblStatusIcon.setIcon(AllIcons.RunConfigurations.ToolbarPassed);
            }
        }));
        runButton.addActionListener(e -> {
            inputComponents.forEach(component -> component.apply(phase.getProcess().getContext()));
            phase.execute(phase.getProcess().getContext());
        });
    }

    private List<InputComponent> inputComponents;

    private void renderInputs() {
        inputComponents = phase.getInputComponent();
        if (CollectionUtils.isEmpty(inputComponents)) {
            return;
        }
        pnlInputs.setLayout(new GridLayoutManager(inputComponents.size(), 1));
        for (int i = 0; i < inputComponents.size(); i++) {
            final InputComponent component = inputComponents.get(i);
            final GridConstraints gridConstraints = new GridConstraints(i, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
            pnlInputs.add(component.getComponent(), gridConstraints);
        }
    }

    private void fillSteps() {
        final List<Step> steps = phase.getSteps();
        if (Collections.isEmpty(steps) || steps.size() == 1) {
            // skip render steps panel for single task
            return;
        }
        stepDecorator = new HideableDecorator(pnlStepsHolder, "Steps", false);
        stepDecorator.setContentComponent(pnlSteps);
        pnlSteps.setLayout(new GridLayoutManager(steps.size(), 1));
        for (int i = 0; i < steps.size(); i++) {
            final Step step = steps.get(i);
            final StepPanel stepPanel = new StepPanel(step);
            final GridConstraints gridConstraints = new GridConstraints(i, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
            pnlSteps.add(stepPanel, gridConstraints);
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
