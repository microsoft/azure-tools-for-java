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

/*
 * Originally copied from com/intellij/execution/configuration/EnvironmentVariablesTextFieldWithBrowseButton.java
 */

package com.microsoft.intellij.runner.springcloud.ui;

import com.intellij.execution.util.EnvVariablesTable;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.UserActivityProviderComponent;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentVariablesTextFieldWithBrowseButton extends TextFieldWithBrowseButton
        implements UserActivityProviderComponent {
    private Map<String, String> environmentVariables = new LinkedHashMap<>();
    private final List<ChangeListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    public EnvironmentVariablesTextFieldWithBrowseButton() {
        super();
        addActionListener(e -> {
            if (this.isEnabled()) {
                final EnvironmentVariablesEditDialog variablesDialog = new EnvironmentVariablesEditDialog(
                        getEnvironmentVariables());
                if (variablesDialog.showAndGet()) {
                    //set text
                    final Map<String, String> newEnvironmentVariables = variablesDialog.getEnvironmentVariables();
                    if (newEnvironmentVariables != null && !newEnvironmentVariables.equals(getEnvironmentVariables())) {
                        setEnvironmentVariables(newEnvironmentVariables);
                        fireStateChanged();
                    }
                }
            }
        });
        getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                if (!StringUtil.equals(stringifyEnvs(environmentVariables), getText())) {
                    fireStateChanged();
                }
            }
        });
    }

    public Map<String, String> getEnvironmentVariables() {
        return EnvVariablesTable.parseEnvsFromText(getText());
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
        this.setText(stringifyEnvs(environmentVariables));
    }

    @NotNull
    @Override
    protected Icon getDefaultIcon() {
        return AllIcons.General.InlineVariables;
    }

    @NotNull
    @Override
    protected Icon getHoveredIcon() {
        return AllIcons.General.InlineVariablesHover;
    }

    @NotNull
    private static String stringifyEnvs(@NotNull Map<String, String> envs) {
        if (envs.isEmpty()) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, String> entry : envs.entrySet()) {
            if (buf.length() > 0) {
                buf.append(";");
            }
            buf.append(StringUtil.escapeChar(entry.getKey(), ';'))
               .append("=")
               .append(StringUtil.escapeChar(entry.getValue(), ';'));
        }
        return buf.toString();
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

    private class EnvironmentVariablesEditDialog extends AzureDialogWrapper {
        private final EnvironmentVariableTable environmentVariableTable;
        private final JPanel pnlRoot;
        private Map<String, String> environmentVariables;

        protected EnvironmentVariablesEditDialog(Map<String, String> environmentVariables) {
            super(EnvironmentVariablesTextFieldWithBrowseButton.this, true);
            environmentVariableTable = new EnvironmentVariableTable();
            environmentVariableTable.setEnv(environmentVariables);

            JLabel label = new JLabel("Environment variables:");
            label.setLabelFor(environmentVariableTable.getTableView().getComponent());

            pnlRoot = new JPanel(new MigLayout("fill, ins 0, gap 0, hidemode 3"));
            pnlRoot.add(label, "hmax pref, wrap");
            pnlRoot.add(environmentVariableTable.getComponent(), "push, grow, wrap, gaptop 5");

            setTitle("Set environment variables for spring cloud app");
            init();
        }

        protected Map<String, String> getEnvironmentVariables() {
            return this.environmentVariables;
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return pnlRoot;
        }

        @Nullable
        @Override
        protected ValidationInfo doValidate() {
            for (EnvironmentVariable variable : environmentVariableTable.getEnvironmentVariables()) {
                String name = variable.getName();
                String value = variable.getValue();
                if (StringUtil.isEmpty(name) && StringUtil.isEmpty(value)) {
                    continue;
                }

                if (!EnvironmentUtil.isValidName(name)) {
                    return new ValidationInfo(String.format("Illegal name of environment variable: %s", name));
                }
                if (!EnvironmentUtil.isValidValue(value)) {
                    return new ValidationInfo(
                            String.format("Value %s is illegal for environment variable %s ", value, name));
                }
            }
            return super.doValidate();
        }

        @Override
        protected void doOKAction() {
            environmentVariableTable.stopEditing();
            environmentVariables = environmentVariableTable.getEnv();
            super.doOKAction();
        }

        @Override
        public void doCancelAction() {
            environmentVariables = null;
            super.doCancelAction();
        }
    }

}
