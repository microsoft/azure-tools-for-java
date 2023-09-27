/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.wizard.module;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.accessibility.AccessibleContextDelegate;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.microsoft.azure.toolkit.intellij.legacy.function.wizard.AzureFunctionsConstants;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.accessibility.AccessibleContext;
import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionTriggerChooserStep extends ModuleWizardStep {
    // only shown v3 bundle template as a workaround
    // todo: @hanli add bundle version options in trigger step
    public static final List<FunctionTemplate> FUNCTION_TEMPLATES = FunctionUtils.loadAllFunctionTemplates().stream()
            .filter(t -> t.isBundleSupported(FunctionExtensionVersion.VERSION_4)).collect(Collectors.toList());
    private final WizardContext wizardContext;
    private CheckBoxList<String> triggerList;
    private static final List<String> INITIAL_SELECTED_TRIGGERS = List.of("HttpTrigger");

    FunctionTriggerChooserStep(final WizardContext wizardContext) {
        super();
        this.wizardContext = wizardContext;
    }

    @Override
    public JComponent getComponent() {
        final FormBuilder builder = new FormBuilder();
        final JBLabel listLabel = new JBLabel("Choose Functions Triggers:");
        builder.addComponent(listLabel);

        triggerList = new CheckBoxList<>() {
            @Override
            public AccessibleContext getAccessibleContext() {
                final AccessibleContext context = super.getAccessibleContext();
                return new AccessibleContextDelegate(context) {
                    @Override
                    public String getAccessibleDescription() {
                        return Stream.of(super.getAccessibleDescription(), "Required")
                                .filter(StringUtils::isNoneBlank)
                                .collect(Collectors.joining(StringUtils.SPACE));
                    }

                    @Override
                    protected Container getDelegateParent() {
                        return triggerList.getParent();
                    }
                };
            }
        };
        listLabel.setLabelFor(triggerList);
        setupFunctionTriggers();

        final BorderLayoutPanel customPanel = JBUI.Panels.simplePanel(10, 0);
        customPanel.addToTop(triggerList);
        builder.addComponent(customPanel);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(builder.getPanel(), "North");
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return panel;
    }

    @Override
    public void updateDataModel() {
        wizardContext.putUserData(AzureFunctionsConstants.WIZARD_TRIGGERS_KEY, getSelectedTriggers().toArray(new FunctionTemplate[0]));
    }

    @Nonnull
    private List<FunctionTemplate> getSelectedTriggers() {
        final DefaultListModel model = (DefaultListModel) triggerList.getModel();
        final int rc = model.getSize();
        final List<String> selectedTriggers = new ArrayList<>();
        for (int ri = 0; ri < rc; ++ri) {
            final JCheckBox checkBox = (JCheckBox) model.getElementAt(ri);
            if (checkBox != null && checkBox.isSelected()) {
                selectedTriggers.add(checkBox.getText());
            }
        }
        return FUNCTION_TEMPLATES.stream()
                .filter(template -> selectedTriggers.contains(template.getName()))
                .collect(Collectors.toList());
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
        FUNCTION_TEMPLATES.stream().map(FunctionTemplate::getName)
                .filter(StringUtils::isNotEmpty) // filter out invalid data
                .map(name -> new JCheckBox(name, INITIAL_SELECTED_TRIGGERS.contains(name)))
                .forEach(model::addElement);
    }
}
