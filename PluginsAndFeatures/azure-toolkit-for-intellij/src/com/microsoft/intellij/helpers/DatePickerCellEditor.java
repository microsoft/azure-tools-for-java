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
package com.microsoft.intellij.helpers;

import com.intellij.ui.JBColor;
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.calendar.DateSelectionModel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;

public abstract class DatePickerCellEditor extends DefaultCellEditor {

    Constructor constructor;
    Object value;

    public DatePickerCellEditor() {
        super(new JTextField());
        this.getComponent().setName("Table.editor");
    }

    @Override
    public boolean stopCellEditing() {
        String var1 = (String) super.getCellEditorValue();

        try {
            if ("".equals(var1)) {
                if (this.constructor.getDeclaringClass() == String.class) {
                    this.value = var1;
                }

                super.stopCellEditing();
            }

            checkAccess(this.constructor.getModifiers());
            this.value = this.constructor.newInstance(var1);
        } catch (Exception var3) {
            ((JComponent) this.getComponent()).setBorder(new LineBorder(JBColor.RED));
            return false;
        }

        return super.stopCellEditing();
    }

    @Override
    public Component getTableCellEditorComponent(JTable jTable, Object value, boolean b, int row, int col) {
        this.value = null;
        ((JComponent) this.getComponent()).setBorder(new LineBorder(JBColor.BLACK));

        try {
            Class columnClass = jTable.getColumnClass(col);
            if (columnClass == Object.class) {
                columnClass = String.class;
            }

            checkPackageAccess(columnClass);
            checkAccess(columnClass.getModifiers());
            this.constructor = columnClass.getConstructor(String.class);
        } catch (Exception ignored) {
            return null;
        }

        final Component component = super.getTableCellEditorComponent(jTable, value, b, row, col);

        if (!isCellDate(jTable, row, col)) {
            return component;
        }

        JButton button = new JButton("...");
        button.setPreferredSize(new Dimension(button.getPreferredSize().height, 40));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final JXMonthView monthView = new JXMonthView();
                monthView.setSelectionMode(DateSelectionModel.SelectionMode.SINGLE_SELECTION);
                monthView.setTraversable(true);
                monthView.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        String date = new SimpleDateFormat().format(monthView.getSelectionDate());
                        ((JTextField) component).setText(date);
                    }
                });

                JDialog frame = new JDialog();
                frame.getContentPane().add(monthView);
                frame.setModal(true);
                frame.setAlwaysOnTop(true);
                frame.setMinimumSize(monthView.getPreferredSize());
                frame.pack();
                frame.setLocation(
                        (Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - frame.getWidth() / 2,
                        (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - frame.getHeight() / 2);

                frame.setVisible(true);
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(component, BorderLayout.CENTER);
        panel.add(button, BorderLayout.LINE_END);

        return panel;

    }

    protected abstract boolean isCellDate(JTable jTable, int row, int col);

    public Object getCellEditorValue() {
        return this.value;
    }

    private static void checkAccess(int modifiers) {
        if (System.getSecurityManager() != null && !Modifier.isPublic(modifiers)) {
            throw new SecurityException("Resource is not accessible");
        }
    }

    private static void checkPackageAccess(Class clazz) {
        checkPackageAccess(clazz.getName());
    }

    private static void checkPackageAccess(String name) {
        SecurityManager securityManager = System.getSecurityManager();

        if (securityManager != null) {
            String classname = name.replace('/', '.');
            int index;

            if (classname.startsWith("[")) {
                index = classname.lastIndexOf('[') + 2;

                if (index > 1 && index < classname.length()) {
                    classname = classname.substring(index);
                }
            }

            index = classname.lastIndexOf('.');

            if (index != -1) {
                securityManager.checkPackageAccess(classname.substring(0, index));
            }
        }
    }
}
