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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.srvpri.SrvPriManager;
import com.microsoft.azuretools.authmanage.srvpri.report.IListener;
import com.microsoft.azuretools.authmanage.srvpri.step.Status;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SrvPriCreationStatusDialog extends AzureDialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(SrvPriCreationStatusDialog.class);

    private JPanel contentPane;
    private JTable statusTable;
    private JList filesList;
    private List<String> authFilePathList = new LinkedList<>();
    String destinationFolder;
    private Map<String, List<String>> tidSidsMap;

    private String selectedAuthFilePath;
    private Project project;
    private final AccessTokenAzureManager preAccessTokenAzureManager;

    public String getSelectedAuthFilePath() {
        return selectedAuthFilePath;
    }

    public List<String> srvPriCreationStatusDialog() {
        return authFilePathList;
    }

    DefaultListModel<String> filesListModel = new DefaultListModel<String>();

    public static SrvPriCreationStatusDialog go(AccessTokenAzureManager preAccessTokenAzureManager,
                                                Map<String, List<String>> tidSidsMap,
                                                String destinationFolder,
                                                Project project) {
        SrvPriCreationStatusDialog d = new SrvPriCreationStatusDialog(preAccessTokenAzureManager, project);
        d.tidSidsMap = tidSidsMap;
        d.destinationFolder = destinationFolder;
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }

        return null;
    }

    private SrvPriCreationStatusDialog(AccessTokenAzureManager preAccessTokenAzureManager, Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.preAccessTokenAzureManager = preAccessTokenAzureManager;
        this.project = project;
        setModal(true);
        setTitle("Create Service Principal Status");

        DefaultTableModel statusTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        statusTableModel.addColumn("Step");
        statusTableModel.addColumn("Result");
        statusTableModel.addColumn("Details");

        statusTable.setModel(statusTableModel);
        statusTable.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
        TableColumn column = statusTable.getColumnModel().getColumn(0);
        column = statusTable.getColumnModel().getColumn(1);
        column.setMinWidth(110);
        column.setMaxWidth(110);

        filesList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        filesList.setLayoutOrientation(JList.VERTICAL);
        filesList.setVisibleRowCount(-1);
        filesList.setModel(filesListModel);
        filesList.setFocusable(false);

        init();
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{this.getOKAction(), this.getCancelAction()};
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        statusTable = new JBTable();
        statusTable.setRowSelectionAllowed(false);
        statusTable.setCellSelectionEnabled(false);
    }

    private void createServicePrincipalsAction() {
        // TODO: this is a temp fix for intelliJ error
        // https://github.com/JetBrains/intellij-community/commit/df6a596e15e2ffb0c2e6b6b4be8c4af0ef096a00#diff-52c7fa7387b3775c006937597017b726
        // The fix should be release in intelliJ 2018. We need revert back to the origin code after the latest two versions are both 2018.
        AzureTaskManager.getInstance().runLater(() -> {
            ActionRunner task = new ActionRunner(project);
            task.queue();
        }, AzureTask.Modality.ANY); // ModalityState.stateForComponent(contentPane));
    }

    private class ActionRunner extends Task.Modal implements IListener<Status> {
        ProgressIndicator progressIndicator = null;

        public ActionRunner(Project project) {
            super(project, "Create Service Principal Progress", true);
        }

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            this.progressIndicator = progressIndicator;
            progressIndicator.setIndeterminate(true);
            progressIndicator.setText("Creating Service Principal for the selected subscription(s)...");
            for (String tid : tidSidsMap.keySet()) {
                if (progressIndicator.isCanceled()) {
                    AzureTaskManager.getInstance().runLater(() -> {
                        DefaultTableModel statusTableModel = (DefaultTableModel) statusTable.getModel();
                        statusTableModel.addRow(new Object[]{"=== Canceled by user", null, null});
                        statusTableModel.fireTableDataChanged();
                    });
                    return;
                }
                List<String> sidList = tidSidsMap.get(tid);
                if (!sidList.isEmpty()) {
                    try {
                        AzureTaskManager.getInstance().runLater(() -> {
                            DefaultTableModel statusTableModel = (DefaultTableModel) statusTable.getModel();
                            statusTableModel.addRow(new Object[]{"tenant ID: " + tid + " ===", null, null});
                            statusTableModel.fireTableDataChanged();
                        });
                        Date now = new Date();
                        String suffix = new SimpleDateFormat("yyyyMMdd-HHmmss").format(now);;
                        final String authFilepath = SrvPriManager.createSp(
                                preAccessTokenAzureManager, tid, sidList, suffix, this, destinationFolder);
                        if (authFilepath != null) {
                            AzureTaskManager.getInstance().runLater(() -> {
                                filesListModel.addElement(authFilepath);
                                filesList.setSelectedIndex(0);
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        LOGGER.error("run@ActionRunner@SrvPriCreationStatusDialog", ex);
                    }
                }
            }
        }

        @Override
        public void listen(final Status status) {
            AzureTaskManager.getInstance().runLater(() -> {
                if (progressIndicator != null) {
                    progressIndicator.setText(status.getAction());
                }

                // if only action was set in the status - the info for progress indicator only - igonre for table
                if (status.getResult() != null) {
                    DefaultTableModel statusTableModel = (DefaultTableModel) statusTable.getModel();
                    statusTableModel.addRow(new Object[]{status.getAction(), status.getResult(), status.getDetails()});
                    statusTableModel.fireTableDataChanged();
                }
            });
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        int rc = filesListModel.getSize();
        if (rc > 0) {
            selectedAuthFilePath = filesListModel.getElementAt(0);
        }

        int[] selectedIndexes = filesList.getSelectedIndices();
        if (selectedIndexes.length > 0) {
            selectedAuthFilePath = filesListModel.getElementAt(selectedIndexes[0]);
        }

        super.doOKAction();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "SrvPriCreationStatusDialog";
    }

    @Override
    public void show() {
        createServicePrincipalsAction();
        super.show();
    }
}

