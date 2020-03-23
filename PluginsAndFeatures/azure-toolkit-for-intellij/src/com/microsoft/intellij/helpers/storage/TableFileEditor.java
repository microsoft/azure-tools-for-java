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

package com.microsoft.intellij.helpers.storage;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.intellij.forms.TableEntityForm;
import com.microsoft.intellij.forms.TablesQueryDesigner;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Table;
import com.microsoft.tooling.msservices.model.storage.TableEntity;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableFileEditor implements FileEditor {
    public static final String PARTITION_KEY = "Partition key";
    public static final String ROW_KEY = "Row key";
    private static final String TIMESTAMP = "Timestamp";

    private static final String EDIT = "Edit";
    private static final String DELETE = "Delete";
    private static final String QUERY = "Query";
    private static final String QUERY_DESIGNER = "QueryDesigner";
    private static final String NEW_ENTITY = "NewEntity";

    private ClientStorageAccount storageAccount;
    private Project project;
    private Table table;
    private JPanel mainPanel;
    private JButton refreshButton;
    private JButton newEntityButton;
    private JButton deleteButton;
    private JTextField queryTextField;
    private JButton queryButton;
    private JButton queryDesignerButton;
    private JTable entitiesTable;
    private List<TableEntity> tableEntities;

    private FileEditorVirtualNode fileEditorVirtualNode;

    public TableFileEditor(final Project project) {
        this.project = project;
        fileEditorVirtualNode = createFileEditorVirtualNode("");
        ActionListener queryActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(QUERY).fireNodeActionEvent();
            }
        };

        queryButton.addActionListener(queryActionListener);
        refreshButton.addActionListener(queryActionListener);

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(DELETE).fireNodeActionEvent();
            }
        });

        newEntityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(NEW_ENTITY).fireNodeActionEvent();
            }
        });

        queryDesignerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileEditorVirtualNode.getNodeActionByName(QUERY_DESIGNER).fireNodeActionEvent();
            }
        });

        entitiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        entitiesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        entitiesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                deleteButton.setEnabled(entitiesTable.getSelectedRows().length > 0);
            }
        });

        entitiesTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getComponent() instanceof JTable) {
                    int r = entitiesTable.rowAtPoint(me.getPoint());

                    if (r >= 0 && r < entitiesTable.getRowCount()) {
                        entitiesTable.setRowSelectionInterval(r, r);
                    } else {
                        entitiesTable.clearSelection();
                    }


                    if (me.getClickCount() == 2) {
                        editEntity();
                    }

                    if (me.getButton() == 3) {
                        JPopupMenu popup = createTablePopUp();
                        popup.show(me.getComponent(), me.getX(), me.getY());
                    }
                }
            }
        });

        entitiesTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    editEntity();
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
            }
        });
    }

    private FileEditorVirtualNode createFileEditorVirtualNode(final String name) {
        FileEditorVirtualNode fileEditorVirtualNode = new FileEditorVirtualNode(this, name);
        fileEditorVirtualNode.addAction(EDIT, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                editEntity();
            }
        });
        fileEditorVirtualNode.addAction(DELETE, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                deleteSelection();
            }
        });
        fileEditorVirtualNode.addAction(QUERY, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                fillGrid();
            }
        });
        fileEditorVirtualNode.addAction(QUERY_DESIGNER, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                final TablesQueryDesigner form = new TablesQueryDesigner(project);
                form.setOnFinish(new Runnable() {
                    @Override
                    public void run() {
                        queryTextField.setText(form.getQueryText());
                    }
                });
                form.show();
            }
        });
        fileEditorVirtualNode.addAction(NEW_ENTITY, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                final TableEntityForm form = new TableEntityForm(project);
                form.setTableName(table.getName());
                form.setStorageAccount(storageAccount);
                form.setTableEntity(null);
                form.setTableEntityList(tableEntities);

                form.setTitle("Add Entity");

                form.setOnFinish(new Runnable() {
                    @Override
                    public void run() {
                        tableEntities.add(form.getTableEntity());

                        refreshGrid();
                    }
                });

                form.show();
            }
        });
        return fileEditorVirtualNode;
    }

    private JPopupMenu createTablePopUp() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(fileEditorVirtualNode.createJMenuItem(EDIT));
        menu.add(fileEditorVirtualNode.createJMenuItem(DELETE));
        return menu;
    }

    private void editEntity() {
        TableEntity[] selectedEntities = getSelectedEntities();

        if (selectedEntities != null && selectedEntities.length > 0) {
            final TableEntity selectedEntity = selectedEntities[0];

            final TableEntityForm form = new TableEntityForm(project);
            form.setTableName(table.getName());
            form.setStorageAccount(storageAccount);
            form.setTableEntity(selectedEntity);

            form.setTitle("Edit Entity");

            form.setOnFinish(new Runnable() {
                @Override
                public void run() {
                    tableEntities.set(entitiesTable.getSelectedRow(), form.getTableEntity());
                    refreshGrid();
                }
            });

            form.show();
        }
    }

    public void fillGrid() {
        final String queryText = queryTextField.getText();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading entities", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                /*try {
                    tableEntities = StorageClientSDKManager.getManager().getTableEntities(storageAccount, table, queryText);
                    refreshGrid();
                } catch (AzureCmdException e) {
                    String msg = "An error occurred while attempting to query entities." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
                }*/
            }
        });
    }

    private void refreshGrid() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Map<String, List<String>> columnData = new LinkedHashMap<String, List<String>>();
                columnData.put(PARTITION_KEY, new ArrayList<String>());
                columnData.put(ROW_KEY, new ArrayList<String>());
                columnData.put(TIMESTAMP, new ArrayList<String>());

                for (TableEntity tableEntity : tableEntities) {
                    columnData.get(PARTITION_KEY).add(tableEntity.getPartitionKey());
                    columnData.get(ROW_KEY).add(tableEntity.getRowKey());
                    columnData.get(TIMESTAMP).add(new SimpleDateFormat().format(tableEntity.getTimestamp().getTime()));

                    for (String entityColumn : tableEntity.getProperties().keySet()) {
                        if (!columnData.keySet().contains(entityColumn)) {
                            columnData.put(entityColumn, new ArrayList<String>());
                        }
                    }

                }

                for (TableEntity tableEntity : tableEntities) {
                    for (String column : columnData.keySet()) {
                        if (!column.equals(PARTITION_KEY) && !column.equals(ROW_KEY) && !column.equals(TIMESTAMP)) {
                            columnData.get(column).add(tableEntity.getProperties().containsKey(column)
                                    ? getFormattedProperty(tableEntity.getProperties().get(column))
                                    : "");
                        }
                    }
                }

                DefaultTableModel model = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int i, int i1) {
                        return false;
                    }
                };

                for (String column : columnData.keySet()) {
                    model.addColumn(column, columnData.get(column).toArray());
                }

                entitiesTable.setModel(model);

                for (int i = 0; i != entitiesTable.getColumnCount(); i++) {
                    entitiesTable.getColumnModel().getColumn(i).setPreferredWidth(100);
                }
            }
        });
    }

    private void deleteSelection() {
        final TableEntity[] selectedEntities = getSelectedEntities();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Deleting entities", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(false);

                /*try {
                    if (selectedEntities != null) {
                        for (int i = 0; i < selectedEntities.length; i++) {
                            progressIndicator.setFraction((double) i / selectedEntities.length);

                            StorageClientSDKManager.getManager().deleteTableEntity(storageAccount, selectedEntities[i]);
                        }

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                tableEntities.removeAll(Arrays.asList(selectedEntities));
                                refreshGrid();
                            }
                        });
                    }
                } catch (AzureCmdException ex) {
                    String msg = "An error occurred while attempting to delete entities." + "\n" + String.format(message("webappExpMsg"), ex.getMessage());
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, ex);
                }*/
            }
        });
    }

    private TableEntity[] getSelectedEntities() {
        if (tableEntities == null) {
            return null;
        }

        int partitionIdIndex = -1;
        int rowIdIndex = -1;

        for (int i = 0; i < entitiesTable.getColumnCount(); i++) {
            String columnName = entitiesTable.getColumnName(i);

            if (columnName.equals(PARTITION_KEY)) {
                partitionIdIndex = i;
            }

            if (columnName.equals(ROW_KEY)) {
                rowIdIndex = i;
            }
        }

        ArrayList<TableEntity> selectedEntities = new ArrayList<TableEntity>();

        for (int i : entitiesTable.getSelectedRows()) {
            for (TableEntity tableEntity : tableEntities) {
                String partitionValue = entitiesTable.getValueAt(i, partitionIdIndex).toString();
                String rowIdValue = entitiesTable.getValueAt(i, rowIdIndex).toString();

                if (tableEntity.getPartitionKey().equals(partitionValue)
                        && tableEntity.getRowKey().equals(rowIdValue)) {
                    selectedEntities.add(tableEntity);
                }
            }
        }

        return selectedEntities.toArray(new TableEntity[selectedEntities.size()]);
    }

    @NotNull
    public static String getFormattedProperty(@NotNull TableEntity.Property property) {
        try {
            switch (property.getType()) {
                case Boolean:
                    return property.getValueAsBoolean().toString();
                case DateTime:
                    return new SimpleDateFormat().format(property.getValueAsCalendar().getTime());
                case Double:
                    return property.getValueAsDouble().toString();
                case Integer:
                    return property.getValueAsInteger().toString();
                case Long:
                    return property.getValueAsLong().toString();
                case Uuid:
                    return property.getValueAsUuid().toString();
                case String:
                    return property.getValueAsString();
            }
        } catch (AzureCmdException ignored) {
        }

        return "";
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setTable(Table table) {
        this.table = table;
        this.fileEditorVirtualNode.setName(table.getName());
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return mainPanel;
    }

    @NotNull
    @Override
    public String getName() {
        return table.getName();
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel fileEditorStateLevel) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @Override
    public void dispose() {
        try {
            unregisterSubscriptionsChanged();
        } catch (AzureCmdException ignored) {
        }
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {
    }

    private void unregisterSubscriptionsChanged()
            throws AzureCmdException {
//        synchronized (subscriptionsChangedSync) {
//            registeredSubscriptionsChanged = false;
//
//            if (subscriptionsChanged != null) {
//                AzureManagerImpl.getManager(project).unregisterSubscriptionsChanged(subscriptionsChanged);
//                subscriptionsChanged = null;
//            }
//        }
    }
}