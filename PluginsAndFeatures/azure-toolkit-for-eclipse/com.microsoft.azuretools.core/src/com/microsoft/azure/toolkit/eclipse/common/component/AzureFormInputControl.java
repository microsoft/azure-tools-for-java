/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.component;

import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import javax.accessibility.AccessibleRelation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public interface AzureFormInputControl<T> extends AzureFormInput<T> {
    String ACCESSIBLE_LABEL = "accessbile.label";
    String VALIDATION_DECORATOR = "azure_validation_decorator";

    default Control getInputControl() {
        return (Control) this;
    }

    /**
     * NOTE: don't override
     */
    @Nonnull
    @Override
    default AzureValidationInfo validateInternal(T value) {
        final Boolean needValidate = AzureTaskManager.getInstance()
            .runAndWaitAsObservable(new AzureTask<>(() -> this.getInputControl().isDisposed() ? false : (!this.getInputControl().isEnabled() || !this.getInputControl().isVisible())))
            .toBlocking().single();
        if (BooleanUtils.isTrue(needValidate)) {
            return AzureValidationInfo.success(this);
        }
        return AzureFormInput.super.validateInternal(value);
    }

    default AzureValidationInfo validateSync() {
        final T value = this.getValue();
        final AzureValidationInfo info = validateInternal(value);
        setValidationInfo(info);
        return info;
    }

    default void setLabeledBy(Label label) {
        this.getInputControl().setData(AccessibleRelation.LABELED_BY, label);
        this.setLabel(label.getText());
    }

    default Label getLabeledBy() {
        return (Label) this.getInputControl().getData(AccessibleRelation.LABELED_BY);
    }

    default void setLabel(String text) {
        String label = text.trim();
        label = label.endsWith(":") ? label.substring(0, label.length() - 1) : label;
        this.set(ACCESSIBLE_LABEL, label);
        final Control control = this.getInputControl();
        control.setData(ACCESSIBLE_LABEL, label);
        control.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(AccessibleEvent e) {
                e.result = getLabel();
            }
        });
    }

    default String getLabel() {
        return Optional.ofNullable((String) this.get(ACCESSIBLE_LABEL)).orElse(AzureFormInput.super.getLabel());
    }

    default void setValidationInfo(@Nullable AzureValidationInfo info) {
        AzureFormInput.super.setValidationInfo(info);
        final Control input = this.getInputControl();
        if (input.isDisposed()) {
            return;
        }
        AzureTaskManager.getInstance().runLater(() -> {
            if (input.isDisposed()) {
                return;
            }
            final ControlDecoration deco = getValidationDecorator(input);
            if (Objects.isNull(info) || info.getType() == AzureValidationInfo.Type.SUCCESS) {
                deco.hide();
            } else {
                deco.setImage(getValidationInfoIcon(info.getType()));
                deco.setDescriptionText("PENDING".equals(info.getMessage()) ? "Validating..." : info.getMessage());
                deco.show();
            }
        });
    }

    @Nonnull
    static ControlDecoration getValidationDecorator(Control input) {
        ControlDecoration deco = (ControlDecoration) input.getData(VALIDATION_DECORATOR);
        if (Objects.isNull(deco)) {
            deco = new ControlDecoration(input, SWT.TOP | SWT.LEAD);
            input.setData(VALIDATION_DECORATOR, deco);
        }
        return deco;
    }

    static Image getValidationInfoIcon(AzureValidationInfo.Type type) {
        if (type == null) {
            return null;
        } else {
            String id = null;
            switch (type) {
                case SUCCESS:
                    id = "DEC_INFORMATION";
                    break;
                case WARNING:
                case PENDING:
                    id = "DEC_WARNING";
                    break;
                case ERROR:
                    id = "DEC_ERROR";
                    break;
            }
            FieldDecoration decoration = FieldDecorationRegistry.getDefault().getFieldDecoration(id);
            return decoration == null ? null : decoration.getImage();
        }
    }
}
