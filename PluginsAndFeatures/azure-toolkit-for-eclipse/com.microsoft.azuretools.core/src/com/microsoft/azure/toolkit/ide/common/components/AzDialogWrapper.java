/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.components;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureFormInputControl;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public abstract class AzDialogWrapper<T> extends TitleAreaDialog {

    public static final String CONTROL_DECORATOR = "azure_validation_decorator";
    private Disposable subscription;

    public AzDialogWrapper(Shell parentShell) {
        super(parentShell);
    }

    @Override
    public void setTitle(String newTitle) {
        super.setTitle(newTitle);
        getShell().setText(newTitle);
    }

    protected void doOkAction() {
        this.setReturnCode(0);
        this.close();
    }

    @Override
    public boolean close() {
        if (Objects.nonNull(this.subscription) && !this.subscription.isDisposed()) {
            this.subscription.dispose();
            this.subscription = null;
        }
        return super.close();
    }

    @Override
    protected final void okPressed() {
        if (revalidate().isEmpty()) {
            doOkAction();
        }
    }

    @Override
    public void create() {
        super.create();
        final List<AzureFormInput<?>> inputs = this.getForm().getInputs();
        for (AzureFormInput<?> input : inputs) {
            ((AzureFormInputControl<?>) input).addValueChangedListener((v) -> {
                this.revalidate();
            });
        }
    }

    public final List<AzureValidationInfo> revalidate() {
        for (AzureFormInput<?> input : this.getForm().getInputs()) {
            final Control control = ((AzureFormInputControl<?>) input).getInputControl();
            ControlDecoration deco = (ControlDecoration) control.getData(CONTROL_DECORATOR);
            if (Objects.nonNull(deco)) {
                deco.hide();
            }
        }
        final List<AzureValidationInfo> errors = this.doValidateAll();
        this.setErrorInfoAll(errors);
        Predicate<List<AzureValidationInfo>> hasPending = (es) -> es.stream().anyMatch(e -> e.getType().equals(AzureValidationInfo.Type.PENDING));
        if (!errors.isEmpty() && hasPending.test(errors)) {
            if (Objects.isNull(this.subscription) || this.subscription.isDisposed()) {
                this.subscription = Flux.interval(Duration.ofMillis(300))
                        .onBackpressureDrop()
                        .publishOn(Schedulers.fromExecutor(command -> AzureTaskManager.getInstance().runLater(command)))
                        .flatMap((i) -> Mono.fromCallable(this::revalidate))
                        .takeUntil(e -> !hasPending.test(e))
                        .subscribe();
            }
        }
        return errors;
    }

    protected final void setErrorInfoAll(List<AzureValidationInfo> infos) {
        for (AzureValidationInfo info : infos) {
            final Control input = ((AzureFormInputControl<?>) info.getInput()).getInputControl();
            ControlDecoration deco = (ControlDecoration) input.getData(CONTROL_DECORATOR);
            if (Objects.isNull(deco)) {
                deco = new ControlDecoration(input, SWT.TOP | SWT.LEAD);
                input.setData(CONTROL_DECORATOR, deco);
            }
            deco.setImage(getValidationInfoIcon(info.getType()));
            deco.setDescriptionText(info.getMessage());
            deco.show();
        }
    }

    public abstract AzureForm<T> getForm();

    protected abstract List<AzureValidationInfo> doValidateAll();

    public static Image getValidationInfoIcon(AzureValidationInfo.Type type) {
        if (type == null) {
            return null;
        } else {
            String id = null;
            switch (type) {
                case INFO:
                    id = "DEC_INFORMATION";
                    break;
                case WARNING:
                    id = "DEC_WARNING";
                    break;
                case PENDING:
                case ERROR:
                    id = "DEC_ERROR";
                    break;
            }
            FieldDecoration decoration = FieldDecorationRegistry.getDefault().getFieldDecoration(id);
            return decoration == null ? null : decoration.getImage();
        }
    }

    public void setOkButtonEnabled(boolean enabled) {
        this.getButton(0).setEnabled(enabled);
    }
}
