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

package com.microsoft.intellij.ui.components;

import com.google.common.collect.ImmutableMap;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.icons.AllIcons;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.JBColor;
import com.intellij.ui.UserActivityProviderComponent;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.table.TableView;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ListTableModel;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This file is based on code in IntelliJ-Community repository, please refer to link below.
 * https://github.com/JetBrains/intellij-community/blob/898213c96ec4d4a9b21d98347c7bd3196f76cdd9/platform/lang-impl/src/com/intellij/execution/configuration/EnvironmentVariablesTextFieldWithBrowseButton.java
 * We removed system environment variables related codes and did some field format changes.
 */
public class JsonEnvPropertiesField extends TextFieldWithBrowseButton implements UserActivityProviderComponent {
    private EnvironmentVariablesData myData = EnvironmentVariablesData.DEFAULT;
    private final Map<String, String> myParentDefaults = new LinkedHashMap<>();
    private final List<ChangeListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private static final String EMPTY_JSON_STRING_PROPERTY = "";

    public JsonEnvPropertiesField() {
        super();
        setEditable(false);
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                setEnvs(ObjectConvertUtils.<String, String>convertJsonToMap(getText()).orElse(new HashMap<>()));
                new JsonEnvPropertiesField.MyEnvironmentVariablesDialog().show();
            }
        });
    }

    /**
     * @return unmodifiable Map instance
     */
    @NotNull
    public Map<String, String> getEnvs() {
        return myData.getEnvs();
    }

    /**
     * @param envs Map instance containing user-defined environment variables
     *             (iteration order should be reliable user-specified, like {@link LinkedHashMap} or {@link ImmutableMap})
     */
    public void setEnvs(@NotNull Map<String, String> envs) {
        setData(EnvironmentVariablesData.create(envs, myData.isPassParentEnvs()));
    }

    @NotNull
    public EnvironmentVariablesData getData() {
        return myData;
    }

    public void setData(@NotNull EnvironmentVariablesData data) {
        EnvironmentVariablesData oldData = myData;
        myData = data;
        setText(stringifyEnvs(data.getEnvs()));
        if (!oldData.equals(data)) {
            fireStateChanged();
        }
    }

    @NotNull
    private static String stringifyEnvs(@NotNull Map<String, String> envs) {
        if (envs.isEmpty()) {
            return EMPTY_JSON_STRING_PROPERTY;
        }
        return ObjectConvertUtils.convertObjectToJsonString(envs).orElse(EMPTY_JSON_STRING_PROPERTY);
    }

    @Override
    public void addChangeListener(@NotNull ChangeListener changeListener) {
        myListeners.add(changeListener);
    }

    @Override
    public void removeChangeListener(@NotNull ChangeListener changeListener) {
        myListeners.remove(changeListener);
    }

    private void fireStateChanged() {
        for (ChangeListener listener : myListeners) {
            listener.stateChanged(new ChangeEvent(this));
        }
    }

    private static List<EnvironmentVariable> convertToVariables(Map<String, String> map, final boolean readOnly) {
        return ContainerUtil.map(map.entrySet(), entry -> new EnvironmentVariable(entry.getKey(), entry.getValue(), readOnly) {
            @Override
            public boolean getNameIsWriteable() {
                return !readOnly;
            }
        });
    }

    private class MyEnvironmentVariablesDialog extends DialogWrapper {
        private static final String TITLE = "Extended Properties";
        private final EnvVariablesTable myUserTable;
        private final JPanel myWholePanel;

        protected MyEnvironmentVariablesDialog() {
            super(JsonEnvPropertiesField.this, true);
            Map<String, String> userMap = new LinkedHashMap<>(myData.getEnvs());

            List<EnvironmentVariable> userList = convertToVariables(userMap, false);
            myUserTable = new JsonEnvPropertiesField.MyEnvVariablesTable(userList, true);

            myWholePanel = new JPanel(new MigLayout("fill, ins 0, gap 0, hidemode 3"));
            myWholePanel.add(myUserTable.getComponent(), "push, grow, wrap, gaptop 5");

            setTitle(TITLE);
            init();
        }

        @Nullable
        @Override
        protected String getDimensionServiceKey() {
            return "EnvironmentVariablesDialog";
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return myWholePanel;
        }

        @Nullable
        @Override
        protected ValidationInfo doValidate() {
            for (EnvironmentVariable variable : myUserTable.getEnvironmentVariables()) {
                String name = variable.getName(), value = variable.getValue();
                if (StringUtil.isEmpty(name) && StringUtil.isEmpty(value)) continue;

                if (!EnvironmentUtil.isValidName(name)) return new ValidationInfo("Illegal property name: " + name);
                if (!EnvironmentUtil.isValidValue(value)) return new ValidationInfo("Illegal property value: " + value);
            }
            return super.doValidate();
        }

        @Override
        protected void doOKAction() {
            myUserTable.stopEditing();
            final Map<String, String> envs = new LinkedHashMap<>();
            for (EnvironmentVariable variable : myUserTable.getEnvironmentVariables()) {
                if (StringUtil.isEmpty(variable.getName()) && StringUtil.isEmpty(variable.getValue())) continue;
                envs.put(variable.getName(), variable.getValue());
            }
            setEnvs(envs);
            super.doOKAction();
        }
    }

    private class MyEnvVariablesTable extends EnvVariablesTable {
        private final boolean myUserList;

        private MyEnvVariablesTable(List<EnvironmentVariable> list, boolean userList) {
            myUserList = userList;
            TableView<EnvironmentVariable> tableView = getTableView();
            tableView.setPreferredScrollableViewportSize(
                    new Dimension(tableView.getPreferredScrollableViewportSize().width,
                            tableView.getRowHeight() * JBTable.PREFERRED_SCROLLABLE_VIEWPORT_HEIGHT_IN_ROWS));
            setValues(list);
            setPasteActionEnabled(myUserList);
        }

        @Nullable
        @Override
        protected AnActionButtonRunnable createAddAction() {
            return myUserList ? super.createAddAction() : null;
        }

        @Nullable
        @Override
        protected AnActionButtonRunnable createRemoveAction() {
            return myUserList ? super.createRemoveAction() : null;
        }

        @NotNull
        @Override
        protected AnActionButton[] createExtraActions() {
            return myUserList
                    ? super.createExtraActions()
                    : ArrayUtil.append(super.createExtraActions(),
                    new AnActionButton(ActionsBundle.message("action.ChangesView.Revert.text"), AllIcons.Actions.Rollback) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            stopEditing();
                            List<EnvironmentVariable> variables = getSelection();
                            for (EnvironmentVariable environmentVariable : variables) {
                                if (isModifiedSysEnv(environmentVariable)) {
                                    environmentVariable.setValue(myParentDefaults.get(environmentVariable.getName()));
                                    setModified();
                                }
                            }
                            getTableView().revalidate();
                            getTableView().repaint();
                        }

                        @Override
                        public boolean isEnabled() {
                            List<EnvironmentVariable> selection = getSelection();
                            for (EnvironmentVariable variable : selection) {
                                if (isModifiedSysEnv(variable)) return true;
                            }
                            return false;
                        }
                    });
        }

        @Override
        protected ListTableModel createListModel() {
            return new ListTableModel(new JsonEnvPropertiesField.MyEnvVariablesTable.MyNameColumnInfo(), new JsonEnvPropertiesField.MyEnvVariablesTable.MyValueColumnInfo());
        }

        protected class MyNameColumnInfo extends EnvVariablesTable.NameColumnInfo {
            private final DefaultTableCellRenderer myModifiedRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row,
                                                               int column) {
                    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    component.setEnabled(table.isEnabled() && (hasFocus || isSelected));
                    return component;
                }
            };

            @Override
            public TableCellRenderer getCustomizedRenderer(EnvironmentVariable o, TableCellRenderer renderer) {
                return o.getNameIsWriteable() ? renderer : myModifiedRenderer;
            }
        }
        protected class MyValueColumnInfo extends EnvVariablesTable.ValueColumnInfo {
            private final DefaultTableCellRenderer myModifiedRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table,
                                                               Object value,
                                                               boolean isSelected,
                                                               boolean hasFocus,
                                                               int row,
                                                               int column) {
                    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    component.setFont(component.getFont().deriveFont(Font.BOLD));
                    if (!hasFocus && !isSelected) {
                        Color linkColor = JBColor.namedColor("Link.activeForeground", JBColor.namedColor("link.foreground", 0x589df6));
                        // TODO: We can replace the linkColor with JBUI.CurrentTheme.Link.linkColor() when IntelliJ upgrade to 2019.1
                        component.setForeground(linkColor);
                    }
                    return component;
                }
            };

            @Override
            public boolean isCellEditable(EnvironmentVariable environmentVariable) {
                return true;
            }

            @Override
            public TableCellRenderer getCustomizedRenderer(EnvironmentVariable o, TableCellRenderer renderer) {
                return isModifiedSysEnv(o) ? myModifiedRenderer : renderer;
            }
        }
    }
    private boolean isModifiedSysEnv(@NotNull EnvironmentVariable v) {
        return !v.getNameIsWriteable() && !Comparing.equal(v.getValue(), myParentDefaults.get(v.getName()));
    }
}