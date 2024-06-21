/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;

import javax.accessibility.AccessibleRelation;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type.PENDING;
import static com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type.SUCCESS;
import static com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type.WARNING;

public interface AzureFormInputComponent<T> extends AzureFormInput<T>, Disposable {
    default JComponent getInputComponent() {
        return (JComponent) this;
    }

    @Override
    default boolean needValidation() {
        final JComponent comp = this.getInputComponent();
        return AzureFormInput.super.needValidation() && comp.isVisible();
    }

    @Override
    default void setValidationInfo(@Nullable AzureValidationInfo vi) {
        AzureFormInput.super.setValidationInfo(vi);
        final ValidationInfo info = Objects.nonNull(vi) && (vi.getType() == PENDING || vi.getType() == SUCCESS) ? null : toIntellijValidationInfo(vi);
        final String state = Objects.isNull(info) ? null : info.warning ? "warning" : "error";
        final JComponent input = this.getInputComponent();
        // see com.intellij.openapi.ui.ComponentValidator.updateInfo
        input.putClientProperty("JComponent.outline", state);
        input.revalidate();
        input.repaint();
        // see com.intellij.openapi.ui.DialogWrapper.setErrorInfoAll
        ComponentValidator.getInstance(input).or(() -> {
            final Optional<ComponentValidator> validator = Optional.ofNullable(new ComponentValidator(AzureFormInputComponent.this).installOn(input));
            Optional.ofNullable(DialogWrapper.findInstance(input)).ifPresent(d -> Disposer.register(d.getDisposable(), this));
            return validator;
        }).ifPresent(v -> AzureTaskManager.getInstance().runLater(() -> v.updateInfo(info), AzureTask.Modality.ANY));
    }

    @Override
    default String getLabel() {
        final JLabel label = (JLabel) this.getInputComponent().getClientProperty(AccessibleRelation.LABELED_BY);
        return Optional.ofNullable(label).map(JLabel::getText)
            .map(t -> t.endsWith(":") ? t.substring(0, t.length() - 1) : t)
            .or(() -> Optional.ofNullable(AzureFormInput.super.getLabel()).filter(StringUtils::isNotBlank))
            .orElse(this.getClass().getSimpleName());
    }

    @Nullable
    static ValidationInfo toIntellijValidationInfo(@Nullable final AzureValidationInfo info) {
        if (Objects.isNull(info)) {
            return null;
        }
        final Object input = info.getInput();
        final JComponent component = input instanceof AzureFormInputComponent<?> ic ? ic.getInputComponent() :
            input instanceof JComponent c ? c : null;
        final ValidationInfo v = new ValidationInfo(Optional.ofNullable(info.getMessage()).orElse("Unknown error"), component);
        if (info.getType() == WARNING) {
            v.asWarning().withOKEnabled();
        }
        return v;
    }

    @Override
    default void dispose() {
        this.clearAll();
    }
}
