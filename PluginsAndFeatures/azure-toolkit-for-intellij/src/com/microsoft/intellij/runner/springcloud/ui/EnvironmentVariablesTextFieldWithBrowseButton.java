/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
            final EnvironmentVariablesEditDialog variablesDialog = new EnvironmentVariablesEditDialog(getEnvironmentVariables());
            if (variablesDialog.showAndGet()) {
                //set text
                final Map<String, String> newEnvironmentVariables = variablesDialog.getEnvironmentVariables();
                if (newEnvironmentVariables != null && !newEnvironmentVariables.equals(getEnvironmentVariables())) {
                    setEnvironmentVariables(newEnvironmentVariables);
                    fireStateChanged();
                }
            }
        });
        getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                if (!StringUtil.equals(stringifyEnvs(environmentVariables), getText())) {
                    Map<String, String> newEnvironmentVariable = EnvVariablesTable.parseEnvsFromText(getText());
                    setEnvironmentVariables(newEnvironmentVariable);
                    fireStateChanged();
                }
            }
        });
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
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

