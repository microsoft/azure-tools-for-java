/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
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
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int i = table.getSelectedRow();
        if (i < 0) {
            DefaultLoader.getUIHelper().showMessageDialog(contentPane, "Please select an artifact", "Select Artifact "
                    + "Status", Messages.getInformationIcon());
            return;
        }
        selectedArtifact = artifactList.get(i);
        super.doOKAction();
    }
}
