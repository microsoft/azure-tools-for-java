/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type.SUCCESS;

@Log
public abstract class AzureDialog<T> extends DialogWrapper {
    protected OkActionListener<T> okActionListener;

    public AzureDialog(Project project) {
        super(project, true);
        setTitle(this.getDialogTitle());
        setModal(true);
    }

    public AzureDialog() {
        this(null);
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

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        final JComponent dft = super.getPreferredFocusedComponent();
        if (Objects.isNull(dft)) {
            return this.getForm().getInputs().stream()
                .filter(i -> i instanceof AzureFormInputComponent)
                .map(i -> ((AzureFormInputComponent<?>) i).getInputComponent())
                .findFirst().orElse(null);
        }
        return dft;
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        final List<AzureValidationInfo> infos = this.getForm().getAllValidationInfos(true);
        // this.setOKActionEnabled(infos.stream().allMatch(AzureValidationInfo::isValid));
        return infos.stream()
            .filter(i -> i.getType() != SUCCESS)
            .map(AzureFormInputComponent::toIntellijValidationInfo)
            .collect(Collectors.toList());
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
