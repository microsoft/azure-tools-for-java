/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.uihelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class InteractiveRenderer extends DefaultTableCellRenderer {
    private int interactiveColumn;

    public InteractiveRenderer(int interactiveColumn) {
        this.interactiveColumn = interactiveColumn;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        InteractiveTableModel tableModel = (InteractiveTableModel)table.getModel();
        if (column == interactiveColumn && hasFocus && tableModel != null) {
            int lastRow = tableModel.getRowCount();
            if (row == lastRow - 1) {
                table.setRowSelectionInterval(lastRow - 1, lastRow - 1);
            } else {
                table.setRowSelectionInterval(row + 1, row + 1);
            }

            table.setColumnSelectionInterval(0, 0);
        }

        return component;
    }
}
