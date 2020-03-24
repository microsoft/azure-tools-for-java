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

package com.microsoft.azuretools.azureexplorer.forms;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azureexplorer.editors.TableFileEditor;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.TableEntity;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class TableEntityForm extends AzureDialogWrapper {
    private Button buttonOK;
    private Button buttonCancel;
    private Button addPropertyButton;
    private Table propertiesTable;

    private TableEntity tableEntity;
    private ClientStorageAccount storageAccount;
    private Runnable onFinish;
    private String tableName;
    private List<TableEntity> tableEntityList;


    private static String[] INVALID_KEYWORDS = {
            "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char", "checked", "class", "const", "continue", "decimal", "default", "delegate", "do", "double", "else",
            "enum", "event", "explicit", "extern", "false", "finally", "fixed", "float", "for", "foreach", "goto", "if", "implicit",
            "in", "int", "interface", "internal", "is", "lock", "long", "namespace", "new", "null", "object", "operator", "out", "override", "params", "private", "protected", "public",
            "readonly", "ref", "return", "sbyte", "sealed", "short", "sizeof", "stackalloc", "static", "string", "struct", "switch", "this", "throw", "true",
            "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using", "virtual", "void", "volatile", "while"
    };

    public TableEntityForm(Shell parentShell, String name) {
        super(parentShell);
        parentShell.setText(name);
    }

    @Override
    protected Control createContents(Composite parent) {

//        DefaultTableModel model = new DefaultTableModel() {
//
//            @Override
//            public boolean isCellEditable(int row, int col) {
//                return (col != 0) && (row > 1 || (col == 3 && tableEntity == null));
//            }
//        };
//
//        model.setColumnIdentifiers(new String[]{
//                "",
//                "Name",
//                "Type",
//                "Value",
//        });
//
//        propertiesTable.setModel(model);
//
//        propertiesTable.getColumn("").setCellRenderer(new DeleteButtonRenderer());
//        propertiesTable.getColumn("").setMaxWidth(30);
//        propertiesTable.getColumn("").setMinWidth(30);
//        propertiesTable.getColumn("Type").setMaxWidth(100);
//        propertiesTable.getColumn("Type").setMinWidth(100);
//        propertiesTable.getColumn("Type").setCellRenderer(new ComboBoxTableRenderer<TableEntity.PropertyType>(TableEntity.PropertyType.values()));
//        propertiesTable.getColumn("Type").setCellEditor(new ComboBoxTableCellEditor());
//
//        propertiesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
//        propertiesTable.getColumn("Value").setCellEditor(new DatePickerCellEditor() {
//            @Override
//            protected boolean isCellDate(JTable table, int row, int col) {
//                return (table.getValueAt(row, 2) == TableEntity.PropertyType.DateTime);
//            }
//        });
//
//        propertiesTable.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent mouseEvent) {
//                int row = propertiesTable.rowAtPoint(mouseEvent.getPoint());
//                int col = propertiesTable.columnAtPoint(mouseEvent.getPoint());
//                if (col == 0 && row > 1) {
//                    ((DefaultTableModel) propertiesTable.getModel()).removeRow(row);
//                }
//            }
//        });



//        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//        addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent e) {
//                onCancel();
//            }
//        });
//
//        contentPane.registerKeyboardAction(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                onCancel();
//            }
//        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        addPropertyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
//                final DefaultTableModel model = (DefaultTableModel) propertiesTable.getModel();
//                model.addRow(new Object[]{
//                        "",
//                        "",
//                        TableEntity.PropertyType.String,
//                        ""
//                });
            }
        });
        return super.createContents(parent);
    }

    public void setTableEntity(TableEntity tableEntity) {
        this.tableEntity = tableEntity;

//        final DefaultTableModel model = (DefaultTableModel) propertiesTable.getModel();
//        model.addRow(new Object[]{
//                "",
//                TableFileEditor.PARTITION_KEY,
//                TableEntity.PropertyType.String,
//                tableEntity == null ? "" : tableEntity.getPartitionKey()
//        });
//
//        model.addRow(new Object[]{
//                "",
//                TableFileEditor.ROW_KEY,
//                TableEntity.PropertyType.String,
//                tableEntity == null ? "" : tableEntity.getRowKey()
//        });
//
//        if (tableEntity != null) {
//            for (String propertyName : tableEntity.getProperties().keySet()) {
//                model.addRow(new Object[]{
//                        "",
//                        propertyName,
//                        tableEntity.getProperties().get(propertyName).getType(),
//                        TableFileEditor.getFormattedProperty(tableEntity.getProperties().get(propertyName))
//                });
//            }
//        }
    }

    @Override
    protected void okPressed() {
//        final TableModel model = propertiesTable.getModel();
//        final String partitionKey = model.getValueAt(0, 3).toString();
//        final String rowKey = model.getValueAt(1, 3).toString();
//        final Map<String, TableEntity.Property> properties = new LinkedHashMap<String, TableEntity.Property>();
//
//        String errors = "";
//
//        for (int row = 2; row != model.getRowCount(); row++) {
//            TableEntity.PropertyType propertyType = (TableEntity.PropertyType) model.getValueAt(row, 2);
//            String name = model.getValueAt(row, 1).toString();
//            String value = model.getValueAt(row, 3).toString();
//
//            if (!isValidPropertyName(name)) {
//                errors = errors + String.format("The property name \"%s\" is invalid\n", name);
//            }
//
//            TableEntity.Property property = getProperty(value, propertyType);
//            if (property == null) {
//                errors = errors + String.format("The field %s has an invalid value for its type.\n", name);
//            } else {
//                properties.put(name, property);
//            }
//        }
//
//        if(tableEntity == null) {
//            for(TableEntity te : tableEntityList) {
//                if(te.getPartitionKey().equals(partitionKey) && te.getRowKey().equals(rowKey)) {
//                    errors = errors + "An entity already exists with this partition key and row key pair.";
//                }
//            }
//        }
//
//        if (errors.length() > 0) {
//            DefaultLoader.getUIHelper().showError(errors, "Service Explorer");
//            return;
//        }
//        String taskName = tableEntity == null ? "Creating entity" : "Updating entity";
//        DefaultLoader.getIdeHelper().runInBackground(null, taskName, false, true, taskName, new Runnable() {
//            public void run() {
//                try {
//                    if (tableEntity == null) {
//                        tableEntity = StorageClientSDKManagerImpl.getManager().createTableEntity(storageAccount,
//                                tableName,
//                                partitionKey,
//                                rowKey,
//                                properties);
//                    } else {
//                        tableEntity.getProperties().clear();
//                        tableEntity.getProperties().putAll(properties);
//                        tableEntity = StorageClientSDKManagerImpl.getManager().updateTableEntity(storageAccount, tableEntity);
//                    }
//
//                    onFinish.run();
//                } catch (AzureCmdException e) {
//                    DefaultLoader.getUIHelper().showException("Error creating entity", e, "Service Explorer", false, true);
//                }
//            }
//        });

        super.okPressed();
    }

    private TableEntity.Property getProperty(String value, TableEntity.PropertyType propertyType) {
        try {
            switch (propertyType) {
                case Boolean:
                    return new TableEntity.Property(Boolean.parseBoolean(value));
                case Integer:
                    return new TableEntity.Property(Integer.parseInt(value));
                case Double:
                    return new TableEntity.Property(Double.parseDouble(value));
                case DateTime:

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(new SimpleDateFormat().parse(value));

                    return new TableEntity.Property(calendar);
                case Uuid:
                    return new TableEntity.Property(UUID.fromString(value));
                case Long:
                    return new TableEntity.Property(Long.parseLong(value));
                default:
                    return new TableEntity.Property(value);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isValidPropertyName(String propertyName) {
        //Validate starting with number
        if (propertyName.matches("^[0-9]\\w*")) {
            return false;
        }
        //Validate special characters
        if (!propertyName.matches("[_\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}][\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}\\p{Mn}\\p{Mc}\\p{Nd}\\p{Pc}\\p{Cf}]*")) {
            return false;
        }
        //Validate invalid keywords
        return !Arrays.asList(INVALID_KEYWORDS).contains(propertyName);
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    public TableEntity getTableEntity() {
        return tableEntity;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setTableEntityList(List<TableEntity> tableEntityList) {
        this.tableEntityList = tableEntityList;
    }

//    private class DeleteButtonRenderer extends DefaultTableCellRenderer {
//        JButton deleteButton;
//
//        public DeleteButtonRenderer() {
//            deleteButton = new JButton();
//            deleteButton.setIcon(UIHelperImpl.loadIcon("storagedelete.png"));
//            deleteButton.setBorderPainted(false);
//        }
//
//        @Override
//        public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int row, int i1) {
//            return (row < 2) ? super.getTableCellRendererComponent(jTable, o, b, b1, row, i1) : deleteButton;
//        }
//    }



}
