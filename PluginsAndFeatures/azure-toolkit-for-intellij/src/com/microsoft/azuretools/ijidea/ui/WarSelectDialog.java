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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class WarSelectDialog extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTable table;

    private List<Artifact> artifactList;
    private Artifact selectedArtifact;

    public Artifact getSelectedArtifact() {
        return selectedArtifact;
    }

    public static WarSelectDialog go(@Nullable Project project, List<Artifact> artifactList) {
        WarSelectDialog d = new WarSelectDialog(project, artifactList);
        d.artifactList = artifactList;
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }

        return null;
    }

    protected WarSelectDialog(@Nullable Project project, List<Artifact> artifactList) {
        super(project, true, IdeModalityType.PROJECT);
        setModal(true);
        setTitle("Select WAR Artifact");

        this.artifactList = artifactList;
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Name");
        tableModel.addColumn("Path");
        for (Artifact artifact : artifactList) {
            tableModel.addRow(new String[] {artifact.getName(), artifact.getOutputFilePath()});
        }
        table.setModel(tableModel);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{this.getOKAction(), this.getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
        int i = table.getSelectedRow();
        if (i < 0) {
            JOptionPane.showMessageDialog(contentPane,
                    "Please select an artifact",
                    "Select Artifact Status",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        selectedArtifact = artifactList.get(i);
        super.doOKAction();
    }
}
