/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.component;

import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AzWizardPageWrapper<T> extends WizardPage {
    private Disposable subscription;
    private int validationDelay = 300;
    private volatile boolean validationEnabled = false;

    protected AzWizardPageWrapper(String pageName) {
        super(pageName);
    }

    @Override
    public void dispose() {
        super.dispose();
        stopValidation();
        validationEnabled = false;
    }

    private void startValidation() {
        stopValidation();

        this.getForm().getInputs().forEach(t -> t.addValueChangedListener(this::valueChanged));
    }

    private void valueChanged(Object o) {
        validateAll();
    }

    private boolean validateAll() {
        if (!validationEnabled) {
            return false;
        }
        final List<AzureValidationInfo> errors = this.doValidateAll();
        boolean valid = Objects.isNull(errors) || errors.isEmpty();
        AzureTaskManager.getInstance().runLater(() -> setErrorInfoAll(errors));
        if (!valid) {
            this.subscription = Mono.delay(Duration.ofMillis(this.validationDelay))
                    .map(n -> this.validateAll())
                    .onErrorStop().subscribe();
        }
        return valid;
    }

    protected final void setErrorInfoAll(List<AzureValidationInfo> infos) {
        final String titleErrorMessage = infos.isEmpty() ? null : infos.get(0).getMessage();
        List<AzureValidationInfo> errors = infos.stream().filter(i -> i.getType() == AzureValidationInfo.Type.ERROR).collect(Collectors.toList());
        this.setErrorMessage(errors.isEmpty() ? null : errors.get(0).getMessage());
        for (AzureValidationInfo info : infos) {
            final AzureFormInputControl<?> input = (AzureFormInputControl<?>) info.getInput();
            input.setValidationInfo(info);
        }
        setOkButtonEnabled(titleErrorMessage == null);
    }

    protected List<AzureValidationInfo> doValidateAll() {
        final List<AzureValidationInfo> infos = this.getForm().getAllValidationInfos(true);
        return infos.stream().filter(i -> !i.isValid()).collect(Collectors.toList());
    }

    public void setValidationDelay(int delay) {
        this.validationDelay = delay;
    }

    public void setOkButtonEnabled(boolean enabled) {
        setPageComplete(enabled);
    }

    public abstract AzureForm<T> getForm();

    @Override
    public IWizardPage getNextPage() {
        // get the code from: https://www.eclipse.org/forums/index.php/t/1008105/ to know the event when user clicks 'next'
        final boolean isNextPressed = "nextPressed".equalsIgnoreCase(Thread.currentThread().getStackTrace()[2].getMethodName());
        if (isNextPressed) {
            final boolean validatedNextPress = validateAll();
            if (!validatedNextPress) {
                return this;
            }
        }

        return super.getNextPage();
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            startValidation();
        }
        validationEnabled = visible;
    }

    private void stopValidation() {
        if (Objects.nonNull(this.subscription) && !this.subscription.isDisposed()) {
            this.subscription.dispose();
            this.subscription = null;
        }
        this.getForm().getInputs().forEach(t -> t.removeValueChangedListener(this::valueChanged));
    }
}
