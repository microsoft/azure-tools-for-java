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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AzWizardPageWrapper<T> extends WizardPage {
    protected AzWizardPageWrapper(String pageName) {
        super(pageName);
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            this.getForm().getInputs().forEach(t -> t.addValueChangedListener(this::valueChanged));
        }
    }

    public boolean isPageComplete() {
        final List<AzureValidationInfo> infos = this.doValidateAllSync();
        return infos.stream().allMatch(AzureValidationInfo::isValid);
    }

    private void valueChanged(Object o) {
        validateAll();
    }

    private boolean validateAll() {
        final List<AzureValidationInfo> errors = this.doValidateAllSync();
        boolean valid = Objects.isNull(errors) || errors.isEmpty();
        AzureTaskManager.getInstance().runLater(() -> showValidationErrors(errors));
        return valid;
    }

    protected final void showValidationErrors(List<AzureValidationInfo> infos) {
        final String titleErrorMessage = infos.isEmpty() ? null : infos.get(0).getMessage();
        List<AzureValidationInfo> errors = infos.stream().filter(i -> i.getType() == AzureValidationInfo.Type.ERROR).collect(Collectors.toList());
        this.setErrorMessage(errors.isEmpty() ? null : errors.get(0).getMessage());
        setOkButtonEnabled(titleErrorMessage == null);
    }

    protected List<AzureValidationInfo> doValidateAllSync() {
        List<AzureValidationInfo> infos = this.getForm().getInputs().stream().map(input ->
                ((AzureFormInputControl<?>) input).validateSync()
        ).collect(Collectors.toList());
        return infos.stream().filter(i -> !i.isValid()).collect(Collectors.toList());
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
}
