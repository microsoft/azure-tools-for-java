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
package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import lombok.extern.java.Log;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Log
public abstract class AzureDialog<T> extends AzureDialogWrapper {
    protected OkActionListener<T> okActionListener;

    public AzureDialog(Project project) {
        super(project, true);
        setTitle(this.getDialogTitle());
        setModal(true);
    }

    public AzureDialog() {
        super(true);
        setTitle(this.getDialogTitle());
        setModal(true);
    }

    @Override
    protected void doOKAction() {
        if (Objects.nonNull(this.okActionListener)) {
            final T data = this.getForm().getData();
            this.okActionListener.onOk(data);
        }
    }

    public void close() {
        this.doCancelAction();
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        final List<AzureValidationInfo> infos = this.getForm().validateData();
        this.setOKActionEnabled(infos.stream().noneMatch(i -> i == AzureValidationInfo.PENDING || i.getType() == AzureValidationInfo.Type.ERROR));
        return infos.stream()
                    .filter(i -> i != AzureValidationInfo.PENDING && i != AzureValidationInfo.OK)
                    .map(AzureDialog::toIntellijValidationInfo)
                    .collect(Collectors.toList());
    }

    //TODO: @wangmi move to some util class
    private static ValidationInfo toIntellijValidationInfo(final AzureValidationInfo info) {
        final AzureFormInput<?> input = info.getInput();
        if (input instanceof AzureFormInputComponent) {
            final JComponent component = ((AzureFormInputComponent<?>) input).getInputComponent();
            return new ValidationInfo(info.getMessage(), component);
        }
        return new ValidationInfo(info.getMessage(), null);
    }

    public abstract AzureForm<T> getForm();

    protected abstract String getDialogTitle();

    public void setOkActionListener(OkActionListener<T> listener) {
        this.okActionListener = listener;
    }

    @FunctionalInterface
    public interface OkActionListener<T> {
        void onOk(T data);
    }
}
