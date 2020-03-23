/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.wizards.functions.module;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.microsoft.intellij.wizards.functions.AzureFunctionsConstants;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FunctionTriggerChooserStep extends ModuleWizardStep {
    private final WizardContext wizardContext;
    private JTable table;
    public static final List<String> SUPPORTED_TRIGGERS = Arrays.asList("HttpTrigger", "BlobTrigger", "QueueTrigger", "TimerTrigger", "EventHubTrigger");
    private static final List<String> INITIAL_SELECTED_TRIGGERS = Arrays.asList("HttpTrigger");

    FunctionTriggerChooserStep(final WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    @Override
    public JComponent getComponent() {
        final FormBuilder builder = new FormBuilder();
        builder.addComponent(new JBLabel("Choose Functions Triggers:"));

        table = new JBTable();
        final DefaultTableModel model = new DefaultTableModel() {
            final Class<?>[] columnClass = new Class[] { Boolean.class, String.class };

            @Override
            public boolean isCellEditable(final int row, final int col) {
                return col == 0;
            }

            @Override
            public Class<?> getColumnClass(final int columnIndex) {
                return columnClass[columnIndex];
            }
        };
        model.addColumn("Selected");
        model.addColumn("Trigger name");
        table.setModel(model);
        setupFunctionTriggers();

        final BorderLayoutPanel customPanel = JBUI.Panels.simplePanel(10, 0);
        customPanel.addToTop(table);
        builder.addComponent(customPanel);

        final TableColumn column = table.getColumnModel().getColumn(0);
        column.setHeaderValue(""); // Don't show title text
        column.setMinWidth(23);
        column.setMaxWidth(23);
        table.getTableHeader().setReorderingAllowed(false);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(builder.getPanel(), "North");
        return panel;
    }

    private void setupFunctionTriggers() {
        final DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (final String trigger : SUPPORTED_TRIGGERS) {
            model.addRow(new Object[] { INITIAL_SELECTED_TRIGGERS.contains(trigger), trigger });
        }
        model.fireTableDataChanged();
    }

    @Override
    public void updateDataModel() {
        wizardContext.putUserData(AzureFunctionsConstants.WIZARD_TRIGGERS_KEY, getSelectedTriggers().toArray(new String[0]));
    }

    @NotNull
    private List<String> getSelectedTriggers() {
        final DefaultTableModel model = (DefaultTableModel) table.getModel();
        final int rc = model.getRowCount();
        final List<String> selectedTriggers = new ArrayList<>();
        for (int ri = 0; ri < rc; ++ri) {
            if (BooleanUtils.isTrue((Boolean) model.getValueAt(ri, 0))) {
                selectedTriggers.add((String) model.getValueAt(ri, 1));
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
}
