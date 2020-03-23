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

package com.microsoft.intellij.forms;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.table.ComboBoxTableCellEditor;
import com.microsoft.intellij.helpers.DatePickerCellEditor;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TablesQueryDesigner extends AzureDialogWrapper {
    private JPanel contentPane;
    private JButton addClauseButton;
    private JTextArea queryTextArea;
    private JTable queryTable;
    private Runnable onFinish;

    public TablesQueryDesigner(Project project) {
        super(project, true);

        setModal(true);
        setTitle("Query Builder");

        DefaultTableModel model = new DefaultTableModel() {

            @Override
            public boolean isCellEditable(int row, int col) {
                return true;
            }
        };

        model.setColumnIdentifiers(new String[]{
                "",
                "And/Or",
                "Property Name",
                "Operation",
                "Value"
        });

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tableModelEvent) {
                updateQueryText();
            }
        });

        queryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queryTable.setModel(model);

        queryTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JTableHeader tableHeader = queryTable.getTableHeader();
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(false);

        queryTable.getColumn("").setCellRenderer(new DeleteButtonRenderer());
        queryTable.getColumn("And/Or").setCellRenderer(new ComboBoxTableRenderer(LogicalOperator.values()));
        queryTable.getColumn("And/Or").setCellEditor(new ComboBoxTableCellEditor());
        queryTable.getColumn("Property Name").setCellRenderer(new ComboBoxTableRenderer(QueryField.values()));
        queryTable.getColumn("Property Name").setCellEditor(new ComboBoxTableCellEditor());
        queryTable.getColumn("Operation").setCellRenderer(new ComboBoxTableRenderer(Operator.values()));
        queryTable.getColumn("Operation").setCellEditor(new ComboBoxTableCellEditor());
        queryTable.getColumn("Value").setCellEditor(new DatePickerCellEditor() {
            @Override
            protected boolean isCellDate(JTable jTable, int row, int col) {
                return jTable.getValueAt(row, 2) == QueryField.Timestamp;
            }
        });


        queryTable.getColumn("").setPreferredWidth(30);
        queryTable.getColumn("And/Or").setPreferredWidth(30);
        queryTable.getColumn("Property Name").setPreferredWidth(100);

        addClauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addClause();
            }
        });

        queryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                int row = queryTable.rowAtPoint(mouseEvent.getPoint());
                int col = queryTable.columnAtPoint(mouseEvent.getPoint());
                if (col == 0) {
                    ((DefaultTableModel) queryTable.getModel()).removeRow(row);
                }
            }
        });

        addClause();

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void updateQueryText() {
        String query = "";

        DefaultTableModel model = (DefaultTableModel) queryTable.getModel();
        for (int i = 0; i != model.getRowCount(); i++) {
            LogicalOperator logicalOperator = (LogicalOperator) model.getValueAt(i, 1);
            QueryField queryField = (QueryField) model.getValueAt(i, 2);
            Operator operator = (Operator) model.getValueAt(i, 3);
            String value = model.getValueAt(i, 4).toString();

            if (queryField == QueryField.Timestamp) {
                try {
                    Date date = new SimpleDateFormat().parse(value);
                    value = "datetime'" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(date) + "'";
                } catch (ParseException ignored) {
                }
            } else {
                value = "'" + value + "'";
            }

            query = query + String.format("%s %s %s %s ",
                    (i == 0) ? "" : logicalOperator.toString().toLowerCase(),
                    queryField.toString(),
                    getOperatorWCF(operator),
                    value);
        }

        queryTextArea.setText(query);
    }

    private void addClause() {
        DefaultTableModel model = (DefaultTableModel) queryTable.getModel();
        model.addRow(new Object[]{
                "",
                LogicalOperator.And,
                QueryField.PartitionKey,
                Operator.EqualsTo,
                ""
        });
    }

    @Override
    protected void doOKAction() {
        onFinish.run();
        sendTelemetry(OK_EXIT_CODE);
        close(DialogWrapper.OK_EXIT_CODE, true);
    }

    private String getOperatorWCF(Operator op) {
        switch (op) {
            case EqualsTo:
                return "eq";
            case GreaterThan:
                return "gt";
            case GreaterThanOrEqualsTo:
                return "ge";
            case LessThan:
                return "lt";
            case LessThanOrEqualsTo:
                return "le";
            case NotEqualsTo:
                return "ne";
        }

        return null;
    }

    public String getQueryText() {
        return queryTextArea.getText();
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    private void createUIComponents() {
        queryTable = new JBTable();
    }


    private enum LogicalOperator {
        And,
        Or
    }

    private enum QueryField {
        PartitionKey,
        RowKey,
        Timestamp
    }

    private enum Operator {
        EqualsTo,
        GreaterThan,
        GreaterThanOrEqualsTo,
        LessThan,
        LessThanOrEqualsTo,
        NotEqualsTo;

        @Override
        public String toString() {
            switch (this) {
                case EqualsTo:
                    return "Equals To";
                case GreaterThan:
                    return "Greater Than";
                case GreaterThanOrEqualsTo:
                    return "Greater Than Or Equals To";
                case LessThan:
                    return "Less Than";
                case LessThanOrEqualsTo:
                    return "Less Than Or Equals To";
                case NotEqualsTo:
                    return "Not Equals To";
            }

            return super.toString();
        }
    }

    private class DeleteButtonRenderer extends DefaultTableCellRenderer {
        JButton deleteButton;

        public DeleteButtonRenderer() {
            deleteButton = new JButton();
            deleteButton.setIcon(UIHelperImpl.loadIcon("storagedelete.png"));
            deleteButton.setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object o, boolean b, boolean b1, int row, int i1) {
            return deleteButton;
        }
    }
}
