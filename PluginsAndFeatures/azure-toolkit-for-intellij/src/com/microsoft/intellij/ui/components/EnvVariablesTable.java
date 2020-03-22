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

import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.execution.util.StringWithNewLinesCellEditor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CopyProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ListTableModel;
import com.microsoft.intellij.ui.components.ListTableWithButtons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This file is based on code in IntelliJ-Community repository, please refer to link below.
 * https://github.com/JetBrains/intellij-community/blob/6e59e93c07a2856995d3d5c76c94afd744de33bb/platform/lang-api/src/com/intellij/execution/util/EnvVariablesTable.java
 * TODO: We're supposed to remove this file and replace this class with `com.intellij.execution.util.EnvVariablesTable` when IntelliJ upgrade to 2019.1
 */
public class EnvVariablesTable extends ListTableWithButtons<EnvironmentVariable> {

    private CopyPasteProviderPanel myPanel;
    private boolean myPasteEnabled = false;

    public EnvVariablesTable() {
        getTableView().getEmptyText().setText("No variables");
        AnAction copyAction = ActionManager.getInstance().getAction(IdeActions.ACTION_COPY);
        if (copyAction != null) {
            copyAction.registerCustomShortcutSet(copyAction.getShortcutSet(), getTableView()); // no need to add in popup menu
        }
        AnAction pasteAction = ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE);
        if (pasteAction != null) {
            pasteAction.registerCustomShortcutSet(pasteAction.getShortcutSet(), getTableView()); // no need to add in popup menu
        }
    }

    public void setPasteActionEnabled(boolean enabled) {
        myPasteEnabled = enabled;
    }

    @Override
    protected ListTableModel createListModel() {
        return new ListTableModel(new NameColumnInfo(), new ValueColumnInfo());
    }

    public void editVariableName(final EnvironmentVariable environmentVariable) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final EnvironmentVariable actualEnvVar = ContainerUtil.find(getElements(),
                    item -> StringUtil.equals(environmentVariable.getName(), item.getName()));
            if (actualEnvVar == null) {
                return;
            }

            setSelection(actualEnvVar);
            if (actualEnvVar.getNameIsWriteable()) {
                editSelection(0);
            }
        });
    }

    public List<EnvironmentVariable> getEnvironmentVariables() {
        return getElements();
    }

    @Override
    public JComponent getComponent() {
        if (myPanel == null) {
            myPanel = new CopyPasteProviderPanel(super.getComponent());
        }
        return myPanel;
    }

    @Override
    protected EnvironmentVariable createElement() {
        return new EnvironmentVariable("", "", false);
    }

    @Override
    protected boolean isEmpty(EnvironmentVariable element) {
        return element.getName().isEmpty() && element.getValue().isEmpty();
    }


    @Override
    protected EnvironmentVariable cloneElement(EnvironmentVariable envVariable) {
        return envVariable.clone();
    }

    @Override
    protected boolean canDeleteElement(EnvironmentVariable selection) {
        return !selection.getIsPredefined();
    }

    protected class NameColumnInfo extends ElementsColumnInfoBase<EnvironmentVariable> {
        public NameColumnInfo() {
            super("Name");
        }
        @Override
        public String valueOf(EnvironmentVariable environmentVariable) {
            return environmentVariable.getName();
        }

        @Override
        public boolean isCellEditable(EnvironmentVariable environmentVariable) {
            return environmentVariable.getNameIsWriteable();
        }

        @Override
        public void setValue(EnvironmentVariable environmentVariable, String s) {
            if (s.equals(valueOf(environmentVariable))) {
                return;
            }
            environmentVariable.setName(s);
            setModified();
        }
        @Override
        protected String getDescription(EnvironmentVariable environmentVariable) {
            return environmentVariable.getDescription();
        }
    }

    protected class ValueColumnInfo extends ElementsColumnInfoBase<EnvironmentVariable> {
        public ValueColumnInfo() {
            super("Value");
        }
        @Override
        public String valueOf(EnvironmentVariable environmentVariable) {
            return environmentVariable.getValue();
        }
        @Override
        public boolean isCellEditable(EnvironmentVariable environmentVariable) {
            return !environmentVariable.getIsPredefined();
        }
        @Override
        public void setValue(EnvironmentVariable environmentVariable, String s) {
            if (s.equals(valueOf(environmentVariable))) {
                return;
            }
            environmentVariable.setValue(s);
            setModified();
        }

        @Nullable
        @Override
        protected String getDescription(EnvironmentVariable environmentVariable) {
            return environmentVariable.getDescription();
        }

        @NotNull
        @Override
        public TableCellEditor getEditor(EnvironmentVariable variable) {
            StringWithNewLinesCellEditor editor = new StringWithNewLinesCellEditor();
            editor.setClickCountToStart(1);
            return editor;
        }
    }

    private class CopyPasteProviderPanel extends JPanel implements DataProvider, CopyProvider, PasteProvider {
        private CopyPasteProviderPanel(JComponent component) {
            super(new GridLayout(1, 1));
            add(component);
        }
        @Nullable
        @Override
        public Object getData(@NotNull String dataId) {
            if (PlatformDataKeys.COPY_PROVIDER.is(dataId) || PlatformDataKeys.PASTE_PROVIDER.is(dataId)) {
                return this;
            }
            return null;
        }

        @Override
        public void performCopy(@NotNull DataContext dataContext) {
            stopEditing();
            StringBuilder sb = new StringBuilder();
            List<EnvironmentVariable> variables = getSelection();
            for (EnvironmentVariable environmentVariable : variables) {
                if (isEmpty(environmentVariable)) continue;
                if (sb.length() > 0) sb.append(';');
                sb.append(StringUtil.escapeChars(environmentVariable.getName(), '=', ';')).append('=')
                        .append(StringUtil.escapeChars(environmentVariable.getValue(), '=', ';'));
            }
            CopyPasteManager.getInstance().setContents(new StringSelection(sb.toString()));
        }


        @Override
        public boolean isCopyEnabled(@NotNull DataContext dataContext) {
            return !getSelection().isEmpty();
        }

        @Override
        public boolean isCopyVisible(@NotNull DataContext dataContext) {
            return isCopyEnabled(dataContext);
        }

        @Override
        public void performPaste(@NotNull DataContext dataContext) {
            String content = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
            Map<String, String> map = parseEnvsFromText(content);
            if (map.isEmpty()) return;
            stopEditing();
            removeSelected();
            List<EnvironmentVariable> parsed = new ArrayList<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                parsed.add(new EnvironmentVariable(entry.getKey(), entry.getValue(), false));
            }
            List<EnvironmentVariable> variables = new ArrayList<>(getEnvironmentVariables());
            variables.addAll(parsed);
            variables = ContainerUtil.filter(variables, variable -> !StringUtil.isEmpty(variable.getName()) ||
                    !StringUtil.isEmpty(variable.getValue()));
            setValues(variables);
        }

        @Override
        public boolean isPastePossible(@NotNull DataContext dataContext) {
            return myPasteEnabled;
        }

        @Override
        public boolean isPasteEnabled(@NotNull DataContext dataContext) {
            return myPasteEnabled;
        }
    }

    @NotNull
    @Override
    protected AnActionButton[] createExtraActions() {
        AnActionButton copyButton = new AnActionButton(ActionsBundle.message("action.EditorCopy.text"), AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                myPanel.performCopy(e.getDataContext());
            }

            @Override
            public boolean isEnabled() {
                return myPanel.isCopyEnabled(DataContext.EMPTY_CONTEXT);
            }
        };
        AnActionButton pasteButton = new AnActionButton(ActionsBundle.message("action.EditorPaste.text"), AllIcons.Actions.Menu_paste) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                myPanel.performPaste(e.getDataContext());
            }

            @Override
            public boolean isEnabled() {
                return myPanel.isPasteEnabled(DataContext.EMPTY_CONTEXT);
            }

            @Override
            public boolean isVisible() {
                return myPanel.isPastePossible(DataContext.EMPTY_CONTEXT);
            }
        };
        return new AnActionButton[]{copyButton, pasteButton};
    }

    public static Map<String, String> parseEnvsFromText(String content) {
        Map<String, String> result = new LinkedHashMap<>();
        if (content != null && content.contains("=")) {
            boolean legacyFormat = content.contains("\n");
            List<String> pairs;
            if (legacyFormat) {
                pairs = StringUtil.split(content, "\n");
            } else {
                pairs = new ArrayList<>();
                int start = 0;
                int end;
                for (end = content.indexOf(";"); end < content.length()-1; end = content.indexOf(";", end+1)) {
                    if (end == -1) {
                        pairs.add(content.substring(start).replace("\\;", ";"));
                        break;
                    }
                    if (end > 0 && (content.charAt(end-1) != '\\')) {
                        pairs.add(content.substring(start, end).replace("\\;", ";"));
                        start = end + 1;
                    }
                }
            }
            for (String pair : pairs) {
                int pos = pair.indexOf('=');
                if (pos == -1) continue;
                while (pos > 0 && pair.charAt(pos - 1) == '\\') {
                    pos = pair.indexOf('=', pos + 1);
                }
                pair = pair.replaceAll("[\\\\]{1}","\\\\\\\\");
                result.put(StringUtil.unescapeStringCharacters(pair.substring(0, pos)),
                        StringUtil.unescapeStringCharacters(pair.substring(pos + 1)));
            }
        }
        return result;
    }
}
