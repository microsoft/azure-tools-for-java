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
package com.microsoft.azuretools.azureexplorer.editors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.azureexplorer.forms.TableEntityForm;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Table;
import com.microsoft.tooling.msservices.model.storage.TableEntity;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

public class TableFileEditor extends EditorPart {
    public static final String PARTITION_KEY = "Partition key";
    public static final String ROW_KEY = "Row key";
    private static final String TIMESTAMP = "Timestamp";

    private static final String EXECUTE = "Execute";
    private static final String DELETE = "Delete";
    private static final String REFRESH = "Refresh";

    private ClientStorageAccount storageAccount;
    private Table table;
    private Button refreshButton;
//    private Button newEntityButton;
    private Button deleteButton;
    private Text queryTextField;
    private Button queryButton;
//    private Button queryDesignerButton;
    private org.eclipse.swt.widgets.Table entitiesTable;
    private TableViewer tableViewer;
    private List<TableEntity> tableEntities;
    private Map<String, List<String>> columnData;
    private List<String> data;

    private FileEditorVirtualNode<EditorPart> fileEditorVirtualNode;

    @Override
    public void doSave(IProgressMonitor iProgressMonitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
//        storageAccount = ((StorageEditorInput) input).getStorageAccount();
        table = (Table) ((StorageEditorInput) input).getItem();
        setPartName(table.getName() + " [Table]");
        fileEditorVirtualNode = createVirtualNode(table.getName());
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    private FileEditorVirtualNode<EditorPart> createVirtualNode(final String name){
        final FileEditorVirtualNode<EditorPart> node = new FileEditorVirtualNode<EditorPart>(this, name);

        node.addAction(DELETE, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                deleteSelection();
            }
        });

        node.addAction(REFRESH, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                fillGrid();
            }
        });

        return node;
    }

    @Override
    public void createPartControl(Composite composite) {
        composite.setLayout(new GridLayout());
        createToolbar(composite);
        createTable(composite);
        createTablePopUp();
    }

    private void createToolbar(Composite parent) {
        GridLayout gridLayout = new GridLayout(2, false);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        Composite container = new Composite(parent, 0);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        queryTextField = new Text(container, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        queryTextField.setLayoutData(gridData);

        RowLayout rowLayout = new RowLayout();
        rowLayout.type = SWT.HORIZONTAL;
        rowLayout.wrap = false;

        Composite buttonsContainer = new Composite(container, SWT.NONE);
        buttonsContainer.setLayout(rowLayout);

        queryButton = new Button(buttonsContainer, SWT.PUSH);
        queryButton.setImage(Activator.getImageDescriptor("icons/Start.png").createImage());
        queryButton.setToolTipText(EXECUTE);

        refreshButton = new Button(buttonsContainer, SWT.PUSH);
        refreshButton.setImage(Activator.getImageDescriptor("icons/storagerefresh.png").createImage());
        refreshButton.setToolTipText(REFRESH);

//        newEntityButton = new Button(buttonsContainer, SWT.PUSH);
//        newEntityButton.setImage(Activator.getImageDescriptor("icons/add_entity.png").createImage());
//        newEntityButton.setToolTipText("Add");

        deleteButton = new Button(buttonsContainer, SWT.PUSH);
        deleteButton.setImage(Activator.getImageDescriptor("icons/storagedelete.png").createImage());
        deleteButton.setToolTipText(DELETE);
        deleteButton.setEnabled(false);

//        queryDesignerButton = new Button(buttonsContainer, SWT.PUSH);
//        queryDesignerButton.setImage(Activator.getImageDescriptor("icons/query_builder.png").createImage());
        SelectionListener queryActionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fileEditorVirtualNode.doAction(REFRESH);
            }
        };

        queryButton.addSelectionListener(queryActionListener);
        refreshButton.addSelectionListener(queryActionListener);

        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fileEditorVirtualNode.doAction(DELETE);
            }
        });

//        newEntityButton.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                final TableEntityForm form = new TableEntityForm(PluginUtil.getParentShell(), "Add Entity");
//                form.setTableName(table.getName());
//                form.setStorageAccount(storageAccount);
//                form.setTableEntity(null);
//                form.setTableEntityList(tableEntities);
//
//                form.setOnFinish(new Runnable() {
//                    @Override
//                    public void run() {
//                        tableEntities.add(form.getTableEntity());
//
//                        refreshGrid();
//                    }
//                });
//                form.open();
//            }
//        });
    }

    private void createTablePopUp() {

    }

    private void editEntity() {
        TableEntity[] selectedEntities = getSelectedEntities();

        if (selectedEntities != null && selectedEntities.length > 0) {
            final TableEntity selectedEntity = selectedEntities[0];

            final TableEntityForm form = new TableEntityForm(PluginUtil.getParentShell(), "Edit Entity");
            form.setTableName(table.getName());
            form.setStorageAccount(storageAccount);
            form.setTableEntity(selectedEntity);

            form.setOnFinish(new Runnable() {
                @Override
                public void run() {
//                    tableEntities.set(entitiesTable.getSelectedRow(), form.getTableEntity());
                    refreshGrid();
                }
            });
            form.open();
        }
    }

    public void fillGrid() {
        final String queryText = queryTextField.getText();

        DefaultLoader.getIdeHelper().runInBackground(null, "Loading entities", false, true, "Loading entities", new Runnable() {
            public void run() {
                /*try {
                    tableEntities = StorageClientSDKManager.getManager().getTableEntities(storageAccount, table, queryText);

                    refreshGrid();
                } catch (AzureCmdException e) {
                    DefaultLoader.getUIHelper().showException("Error querying entities", e, "Service Explorer", false, true);
                }*/
            }
        });
    }

    private void refreshGrid() {
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                columnData = new LinkedHashMap<String, List<String>>();
                data = new ArrayList<String>();
                data.add(PARTITION_KEY);
                data.add(ROW_KEY);
                data.add(TIMESTAMP);
                columnData.put(PARTITION_KEY, new ArrayList<String>());
                columnData.put(ROW_KEY, new ArrayList<String>());
                columnData.put(TIMESTAMP, new ArrayList<String>());

                for (TableEntity tableEntity : tableEntities) {
                    columnData.get(PARTITION_KEY).add(tableEntity.getPartitionKey());
                    columnData.get(ROW_KEY).add(tableEntity.getRowKey());
                    columnData.get(TIMESTAMP).add(new SimpleDateFormat().format(tableEntity.getTimestamp().getTime()));

                    for (String entityColumn : tableEntity.getProperties().keySet()) {
                        if (!data.contains(entityColumn)) {
                            data.add(entityColumn);
                        }
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
                entitiesTable.setRedraw(false);
                while (entitiesTable.getColumnCount() > 0 ) {
                    entitiesTable.getColumns()[0].dispose();
                }
                for (String columnName : data) {
                    TableColumn tableColumn = new TableColumn(entitiesTable, SWT.FILL);
                    tableColumn.setText(columnName);
                    tableColumn.setWidth(100);
                }
                entitiesTable.setRedraw(true);

                tableViewer.setInput(tableEntities);
            }
        });
    }

    private void deleteSelection() {
        final TableEntity[] selectedEntities = getSelectedEntities();

        Job job = new Job("Deleting entities") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Deleting entities", IProgressMonitor.UNKNOWN);
//                try {
                    if (selectedEntities != null) {
                        for (int i = 0; i < selectedEntities.length; i++) {
                            monitor.worked(i * 100 / selectedEntities.length);

//                            StorageClientSDKManager.getManager().deleteTableEntity(storageAccount, selectedEntities[i]);
                        }
                        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                tableEntities.removeAll(Arrays.asList(selectedEntities));

                                refreshGrid();
                            }
                        });
                    }
                    return Status.OK_STATUS;
//                } catch (AzureCmdException ex) {
//                    DefaultLoader.getUIHelper().showException("Error deleting entities", ex, "Service Explorer", false, true);
//                    return Status.CANCEL_STATUS;
//                }
            }
        };
        job.schedule();
    }

    private TableEntity[] getSelectedEntities() {
        if (tableEntities == null) {
            return null;
        }

        int partitionIdIndex = -1;
        int rowIdIndex = -1;

//        for (int i = 0; i < entitiesTable.getColumnCount(); i++) {
//            String columnName = entitiesTable.getColumnName(i);
//
//            if (columnName.equals(PARTITION_KEY)) {
//                partitionIdIndex = i;
//            }
//
//            if (columnName.equals(ROW_KEY)) {
//                rowIdIndex = i;
//            }
//        }

        ArrayList<TableEntity> selectedEntities = new ArrayList<TableEntity>();

//        for (int i : entitiesTable.getSelectedRows()) {
//            for (TableEntity tableEntity : tableEntities) {
//                String partitionValue = entitiesTable.getValueAt(i, partitionIdIndex).toString();
//                String rowIdValue = entitiesTable.getValueAt(i, rowIdIndex).toString();
//
//                if (tableEntity.getPartitionKey().equals(partitionValue)
//                        && tableEntity.getRowKey().equals(rowIdValue)) {
//                    selectedEntities.add(tableEntity);
//                }
//            }
//        }

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

    private void createTable(Composite parent) {
        entitiesTable = new org.eclipse.swt.widgets.Table(parent, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);

        entitiesTable.setHeaderVisible(true);
        entitiesTable.setLinesVisible(true);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.grabExcessHorizontalSpace = true;
        entitiesTable.setLayoutData(gridData);

        tableViewer = new TableViewer(entitiesTable);
        tableViewer.setUseHashlookup(true);
        tableViewer.setContentProvider(new TableContentProvider());
        tableViewer.setLabelProvider(new TableLabelProvider());

        fillGrid();
    }

    @Override
    public void setFocus() {

    }

    private class TableContentProvider implements IStructuredContentProvider {
        public void dispose() {

        }

        public void inputChanged(Viewer viewer, Object o, Object o1) {

        }

        public Object[] getElements(Object o) {
            return tableEntities.toArray();
        }
    }

    private class TableLabelProvider implements ITableLabelProvider {
        public void addListener(ILabelProviderListener iLabelProviderListener) {

        }

        public void dispose() {

        }

        public boolean isLabelProperty(Object o, String s) {
            return false;
        }

        public void removeListener(ILabelProviderListener iLabelProviderListener) {

        }

        public Image getColumnImage(Object o, int i) {
            return null;
        }

        public String getColumnText(Object o, int i) {
            String key = data.get(i);
            if (key == null) {
                return null;
            }
            TableEntity tableEntity = (TableEntity) o;
            if (key.equals(PARTITION_KEY)) {
                return tableEntity.getPartitionKey();
            } else if (key.equals(ROW_KEY)) {
                return tableEntity.getRowKey();
            } else if (key.equals(TIMESTAMP)) {
                return new SimpleDateFormat().format(tableEntity.getTimestamp().getTime());
            } else {
                return tableEntity.getProperties().containsKey(key)
                        ? getFormattedProperty(tableEntity.getProperties().get(key))
                        : "";
            }
        }
    }
}
