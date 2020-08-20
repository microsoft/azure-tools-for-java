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

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.table.JBTable;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.utility.JTableUtils;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;

public class SrvPriSettingsDialog extends AzureDialogWrapper {
    private static final int CHECKBOX_COLUMN_INDEX = 0;

    private JPanel contentPane;
    private JTable table;
    private JTextPane selectSubscriptionCommentTextPane;
    private TextFieldWithBrowseButton destinationFolderTextField;
    private final List<SubscriptionDetail> sdl;
    private final Project project;

    public String getDestinationFolder() {
        return destinationFolderTextField.getText();
    }

    public List<SubscriptionDetail> getSubscriptionDetails() {
        return sdl;
    }

    DefaultTableModel model = new DefaultTableModel() {
        final Class[] columnClass = new Class[]{Boolean.class, String.class, String.class};

        @Override
        public boolean isCellEditable(int row, int col) {
            return (col == 0);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClass[columnIndex];
        }
    };

    public static SrvPriSettingsDialog go(List<SubscriptionDetail> sdl, Project project) throws Exception {
        final SrvPriSettingsDialog d = new SrvPriSettingsDialog(sdl, project);
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }

        return null;
    }

    private SrvPriSettingsDialog(List<SubscriptionDetail> sdl, Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.sdl = sdl;
        this.project = project;

        setModal(true);
        setTitle("Create Authentication Files");
        setOKButtonText("Start");

        model.addColumn("");
        model.addColumn("Subscription name");
        model.addColumn("Subscription id");

        table.setModel(model);

        final TableColumn column = table.getColumnModel().getColumn(CHECKBOX_COLUMN_INDEX);
        column.setMinWidth(23);
        column.setMaxWidth(23);
        JTableUtils.enableBatchSelection(table, CHECKBOX_COLUMN_INDEX);

        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(false);

        destinationFolderTextField.setText(System.getProperty("user.home"));
        destinationFolderTextField.addBrowseFolderListener("Choose Destination Folder", "", null,
                                                           FileChooserDescriptorFactory.createSingleFolderDescriptor());

        final Font labelFont = UIManager.getFont("Label.font");
        selectSubscriptionCommentTextPane.setFont(labelFont);

        setSubscriptions();

        init();
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{this.getOKAction(), this.getCancelAction()};
    }

    private void setSubscriptions() {
        for (final SubscriptionDetail sd : sdl) {
            model.addRow(new Object[] {sd.isSelected(), sd.getSubscriptionName(), sd.getSubscriptionId()});
            model.fireTableDataChanged();
        }
    }

    private void createUIComponents() {
        table = new JBTable();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        final int rc = model.getRowCount();
        int unselectedCount = 0;
        for (int ri = 0; ri < rc; ++ri) {
            final boolean selected = (boolean) model.getValueAt(ri, 0);
            if (!selected) {
                unselectedCount++;
            }
        }

        if (unselectedCount == rc) {
            DefaultLoader.getUIHelper().showMessageDialog(contentPane, "Please select at least one subscription",
                                                          "Subscription Dialog Status", Messages.getInformationIcon());
            return;
        }

        for (int ri = 0; ri < rc; ++ri) {
            final boolean selected = (boolean) model.getValueAt(ri, 0);
            this.sdl.get(ri).setSelected(selected);
        }

        super.doOKAction();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "SrvPriSettingsDialog";
    }

}
