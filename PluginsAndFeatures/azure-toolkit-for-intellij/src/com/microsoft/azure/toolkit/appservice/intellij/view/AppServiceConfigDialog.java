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

package com.microsoft.azure.toolkit.appservice.intellij.view;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.appservice.AppServiceConfig;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;

import javax.swing.*;
import java.util.Objects;

public abstract class AppServiceConfigDialog<T extends AppServiceConfig>
        extends AzureDialogWrapper
        implements AzureForm<T> {
    public static final String LABEL_ADVANCED_MODE = "More settings";
    protected Project project;
    private JCheckBox checkboxMode;
    private boolean advancedMode = false;
    protected OkActionListener<T> okActionListener;

    public AppServiceConfigDialog(Project project) {
        super(project, true);
        this.project = project;
        setTitle(this.getDialogTitle());
        setModal(true);
    }

    protected void toggleAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
        final AzureFormPanel<T> basicForm = this.getBasicFormPanel();
        final AzureFormPanel<T> advancedForm = this.getAdvancedFormPanel();
        if (advancedMode) {
            basicForm.setVisible(false);
            advancedForm.setVisible(true);
        } else {
            basicForm.setVisible(true);
            advancedForm.setVisible(false);
        }
        this.repaint();
    }

    public void setOkActionListener(OkActionListener<T> listener) {
        this.okActionListener = listener;
    }

    @Override
    protected void doOKAction() {
        if (Objects.nonNull(this.okActionListener)) {
            final T data = this.getData();
            this.okActionListener.onOk(data);
        }
    }

    public void close() {
        this.doCancelAction();
    }

    @Override
    protected JComponent createDoNotAskCheckbox() {
        this.checkboxMode = new JCheckBox(LABEL_ADVANCED_MODE);
        this.checkboxMode.setVisible(true);
        this.checkboxMode.setSelected(false);
        this.checkboxMode.addActionListener(e -> this.toggleAdvancedMode(this.checkboxMode.isSelected()));
        return this.checkboxMode;
    }

    @Override
    public T getData() {
        return this.getForm().getData();
    }

    public AzureForm<T> getForm() {
        return this.advancedMode ? this.getAdvancedFormPanel() : this.getBasicFormPanel();
    }

    protected abstract AzureFormPanel<T> getAdvancedFormPanel();

    protected abstract AzureFormPanel<T> getBasicFormPanel();

    protected abstract String getDialogTitle();

    @FunctionalInterface
    public interface OkActionListener<T extends AppServiceConfig> {
        void onOk(T data);
    }
}
