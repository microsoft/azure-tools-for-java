package com.microsoft.azure.toolkit.intellij.appservice.input;

import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppNameInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class AppServiceNameInputPanel implements AzureFormJPanel<String> {
    private JPanel rootPanel;
    private AppNameInput appNameInput;

    public AppServiceNameInputPanel() {
        $$$setupUI$$$();
        this.appNameInput.setRequired(true);
    }

    @Override
    public String getValue() {
        return appNameInput.getValue();
    }

    public void setValue(final String value) {
        this.appNameInput.setValue(value);
    }

    public void setSubscription(@Nonnull final Subscription subscription) {
        this.appNameInput.setSubscription(subscription);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(appNameInput);
    }

    @Override
    public JPanel getContentPanel() {
        return rootPanel;
    }
}
