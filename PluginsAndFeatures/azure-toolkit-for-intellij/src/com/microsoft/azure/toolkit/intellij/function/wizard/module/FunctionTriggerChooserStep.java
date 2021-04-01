/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.wizard.module;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.microsoft.azure.toolkit.intellij.function.wizard.AzureFunctionsConstants;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FunctionTriggerChooserStep extends ModuleWizardStep {
    public static final List<String> SUPPORTED_TRIGGERS = Arrays.asList("HttpTrigger", "BlobTrigger", "QueueTrigger", "TimerTrigger", "EventHubTrigger");
    private final WizardContext wizardContext;
    private CheckBoxList<String> triggerList;
    private static final List<String> INITIAL_SELECTED_TRIGGERS = Arrays.asList("HttpTrigger");

    FunctionTriggerChooserStep(final WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    @Override
    public JComponent getComponent() {
        final FormBuilder builder = new FormBuilder();
        builder.addComponent(new JBLabel("Choose Functions Triggers:"));

        triggerList = new CheckBoxList<>();
        setupFunctionTriggers();

        final BorderLayoutPanel customPanel = JBUI.Panels.simplePanel(10, 0);
        customPanel.addToTop(triggerList);
        builder.addComponent(customPanel);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(builder.getPanel(), "North");
        return panel;
    }

    @Override
    public void updateDataModel() {
        wizardContext.putUserData(AzureFunctionsConstants.WIZARD_TRIGGERS_KEY, getSelectedTriggers().toArray(new String[0]));
    }

    @NotNull
    private List<String> getSelectedTriggers() {
        final DefaultListModel model = (DefaultListModel) triggerList.getModel();
        final int rc = model.getSize();
        final List<String> selectedTriggers = new ArrayList<>();
        for (int ri = 0; ri < rc; ++ri) {
            final JCheckBox checkBox = (JCheckBox) model.getElementAt(ri);
            if (checkBox != null && checkBox.isSelected()) {
                selectedTriggers.add(checkBox.getText());
            }
        }
        return selectedTriggers;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        if (getSelectedTriggers().isEmpty()) {
            throw new ConfigurationException("Must select at least one trigger.");
        }
        return true;
    }

    private void setupFunctionTriggers() {
        final DefaultListModel model = (DefaultListModel) triggerList.getModel();
        for (final String trigger : SUPPORTED_TRIGGERS) {
            model.addElement(new JCheckBox(trigger, INITIAL_SELECTED_TRIGGERS.contains(trigger)));
        }
    }
}
