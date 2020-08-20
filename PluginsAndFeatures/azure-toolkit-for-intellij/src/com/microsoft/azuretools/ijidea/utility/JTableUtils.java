package com.microsoft.azuretools.ijidea.utility;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;

public class JTableUtils {
    public static void enableBatchSelection(JTable table, int checkboxColumnIndex) {
        final TableColumn column = table.getColumnModel().getColumn(checkboxColumnIndex);
        final CheckBoxHeaderRenderer checkboxHeader = new CheckBoxHeaderRenderer();
        column.setHeaderRenderer(checkboxHeader);
        checkboxHeader.addItemListener(new CheckboxHeaderStateListener(table, checkboxColumnIndex));
//        table.getModel().addTableModelListener(new CheckBoxModelListener(table, checkboxColumnIndex));
    }

    private static class CheckBoxModelListener implements TableModelListener, ItemListener {
        private final JTable table;
        private final int colIndex;
        private final JCheckBox checkbox;

        public CheckBoxModelListener(final JTable table, final int columnIndex) {
            this.table = table;
            this.colIndex = columnIndex;
            final TableColumn column = table.getColumnModel().getColumn(columnIndex);
            this.checkbox = (JCheckBox) column.getHeaderRenderer();
            this.checkbox.addItemListener(this);
        }

        @Override
        public void tableChanged(final TableModelEvent e) {
            final int column = e.getColumn();
            final TableModel model = (TableModel) e.getSource();
            if (column == this.colIndex) {
                boolean allSelected = true;
                for (int row = 0; row < model.getRowCount(); row++) {
                    allSelected = allSelected && (Boolean) model.getValueAt(row, this.colIndex);
                }
                this.checkbox.getModel().setSelected(allSelected);
            }
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            final Object source = e.getSource();
            if (!(source instanceof AbstractButton)) {
                return;
            }
            final boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            final int rowCount = table.getModel().getRowCount();
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                table.getModel().setValueAt(checked, rowIndex, this.colIndex);
            }
        }
    }

    static class CheckBoxHeaderRenderer extends JCheckBox implements TableCellRenderer, MouseListener {
        private int column = -1;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (this.column == -1) {
                this.column = column;
                final JTableHeader header = table.getTableHeader();
                this.setForeground(header.getForeground());
                this.setBackground(header.getBackground());
                header.addMouseListener(this);
            }
            return this;
        }

        public void mouseClicked(MouseEvent e) {
            final JTableHeader header = (JTableHeader) (e.getSource());
            final JTable table = header.getTable();
            final int columnIndex = table.columnAtPoint(e.getPoint());

            if (columnIndex == this.column) {
                doClick();
            }
            header.repaint();
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }

    private static class CheckboxHeaderStateListener implements ItemListener {
        private final JTable table;
        private final int checkboxColumnIndex;

        public CheckboxHeaderStateListener(JTable table, int checkboxColumnIndex) {
            this.table = table;
            this.checkboxColumnIndex = checkboxColumnIndex;
        }

        public void itemStateChanged(ItemEvent e) {
            final Object source = e.getSource();
            if (!(source instanceof AbstractButton)) {
                return;
            }
            final boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            final int rowCount = table.getModel().getRowCount();
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                table.getModel().setValueAt(checked, rowIndex, this.checkboxColumnIndex);
            }
        }
    }
}
