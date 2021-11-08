/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.ComponentsKt;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class RegisterAzureApplicationForm implements AzureFormJPanel<ApplicationRegistrationModel> {
    private JPanel contentPanel;

    private AzureTextInput displayNameInput;
    private AzureTextInput domainInput;
    private JBCheckBox multiTenantInput;
    private AzureTextInput clientIdInput;
    private JPanel advancedSettingsContentPanel;
    private TitledSeparator advancedSettingsSeparator;
    private JComponent noteComponent;
    private JBLabel clientIdNote;
    private SubscriptionComboBox subscriptionBox;
    private JComponent callbackUrls;

    // initialized in createUIComponents
    private AzureCallbackUrlTable callbackUrlsTable;

    RegisterAzureApplicationForm(@Nonnull Project project) {
        var separator = (AzureHideableTitledSeparator) advancedSettingsSeparator;
        separator.addContentComponent(advancedSettingsContentPanel);
        separator.collapse();

        clientIdNote.setAllowAutoWrapping(true);

        subscriptionBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateRegistrationModel(project, (Subscription) e.getItem());
            }
        });
    }

    JComponent getPreferredFocusedComponent() {
        return subscriptionBox;
    }

    @Override
    public JPanel getContentPanel() {
        return contentPanel;
    }

    @Override
    public ApplicationRegistrationModel getValue() {
        var urls = callbackUrlsTable.getTableView().getItems()
                .stream()
                .map(StringBuilder::toString)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        var data = new ApplicationRegistrationModel();
        data.setDisplayName(displayNameInput.getText());
        data.setCallbackUrls(urls);
        data.setDomain(domainInput.getText());
        data.setMultiTenant(multiTenantInput.isSelected());
        data.setClientId(clientIdInput.getText());
        return data;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(subscriptionBox, displayNameInput, callbackUrlsTable, domainInput, clientIdInput);
    }

    @Override
    public void setValue(ApplicationRegistrationModel data) {
        var callbackUrls = data.getCallbackUrls();

        displayNameInput.setText(data.getDisplayName());
        callbackUrlsTable.setValues(callbackUrls.stream().map(StringBuilder::new).collect(Collectors.toList()));
        domainInput.setText(data.getDomain());
        multiTenantInput.setSelected(data.isMultiTenant());
        clientIdInput.setText(data.getClientId());
    }

    private void createUIComponents() {
        noteComponent = ComponentsKt.noteComponent(MessageBundle.message("dialog.identity.ad.register_app.description"));
        noteComponent.setBorder(JBUI.Borders.emptyBottom(5));

        subscriptionBox = new SubscriptionComboBox();
        subscriptionBox.setEditable(false);
        subscriptionBox.setRequired(true);

        callbackUrlsTable = new AzureCallbackUrlTable();
        callbackUrlsTable.getTableView().setTableHeader(null);
        callbackUrls = callbackUrlsTable.getComponent();
        callbackUrls.setBorder(JBUI.Borders.merge(callbackUrls.getBorder(), JBUI.Borders.empty(0, 4), true));

        clientIdInput = new AzureClientIdInput();
        advancedSettingsSeparator = new AzureHideableTitledSeparator();
    }

    private void updateRegistrationModel(@Nonnull Project project, @Nonnull Subscription subscription) {
        var title = MessageBundle.message("action.azure.aad.registerApp.loadDefaultDomain");
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);

            var client = AzureUtils.createGraphClient(subscription);
            var domain = AzureUtils.loadDomains(client)
                    .stream()
                    .filter(d -> d.isDefault)
                    .map(d -> d.id)
                    .findFirst()
                    .orElse("");

            var model = new ApplicationRegistrationModel();
            model.setDomain(domain);
            model.setCallbackUrls(List.of(ApplicationRegistrationModel.DEFAULT_CALLBACK_URL));

            AzureTaskManager.getInstance().runLater(() -> setValue(model));
        }));
    }

    @Nullable
    public Subscription getSubscription() {
        return subscriptionBox.getValue();
    }
}
