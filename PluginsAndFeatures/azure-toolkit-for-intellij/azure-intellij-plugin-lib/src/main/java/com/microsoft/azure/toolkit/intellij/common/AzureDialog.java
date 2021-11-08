/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type.*;

@Log
public abstract class AzureDialog<T> extends DialogWrapper {
    protected OkActionListener<T> okActionListener;

    public AzureDialog(Project project) {
        super(project, true);
        setTitle(this.getDialogTitle());
        setModal(true);
    }

    public AzureDialog() {
        super(null);
    }

    @Override
    protected void doOKAction() {
        try {
            if (Objects.nonNull(this.okActionListener)) {
                final T data = this.getForm().getValue();
                this.okActionListener.onOk(data);
            } else {
                super.doOKAction();
            }
        } catch (final Exception e) {
            AzureMessager.getMessager().error(e);
        }
    }

    public void close() {
        this.doCancelAction();
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        final List<AzureValidationInfo> infos = this.getForm().getAllValidationInfos(true);
        this.setOKActionEnabled(infos.stream().noneMatch(i -> i.getType() == PENDING || i.getType() == ERROR));
        return infos.stream()
            .filter(i -> i.getType() != SUCCESS)
            .map(AzureDialog::toIntellijValidationInfo)
            .collect(Collectors.toList());
    }

    private static ValidationInfo toIntellijValidationInfo(final AzureValidationInfo info) {
        final AzureFormInput<?> input = info.getInput();
        final JComponent component = input instanceof AzureFormInputComponent ? ((AzureFormInputComponent<?>) input).getInputComponent() : null;
        final ValidationInfo v = new ValidationInfo(info.getType() == PENDING ? StringUtils.EMPTY : info.getMessage(), component);
        if (info.getType() == WARNING) {
            v.asWarning();
        }
        return v;
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
