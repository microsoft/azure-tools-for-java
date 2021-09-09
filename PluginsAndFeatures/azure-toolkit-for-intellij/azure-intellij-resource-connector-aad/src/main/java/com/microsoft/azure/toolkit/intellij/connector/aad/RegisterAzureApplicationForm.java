package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.ComponentsKt;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

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
    private AzureEditableCallbackUrlsCombobox callbackUrlsInput;

    RegisterAzureApplicationForm() {
        var separator = (AzureHideableTitledSeparator) advancedSettingsSeparator;
        separator.addContentComponent(advancedSettingsContentPanel);
        separator.collapse();

        clientIdNote.setAllowAutoWrapping(true);
    }

    JComponent getPreferredFocusedComponent() {
        return displayNameInput;
    }

    @Override
    public JPanel getContentPanel() {
        return contentPanel;
    }

    @Override
    public ApplicationRegistrationModel getData() {
        var data = new ApplicationRegistrationModel();
        data.setDisplayName(displayNameInput.getText());
        data.setCallbackUrls(callbackUrlsInput.getItems());
        data.setDomain(domainInput.getText());
        data.setMultiTenant(multiTenantInput.isSelected());
        data.setClientId(clientIdInput.getText());
        return data;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(displayNameInput, callbackUrlsInput, domainInput, clientIdInput);
    }

    @Override
    public void setData(ApplicationRegistrationModel data) {
        var callbackUrls = data.getCallbackUrls();

        displayNameInput.setText(data.getDisplayName());
        callbackUrlsInput.setUrls(callbackUrls);
        domainInput.setText(data.getDomain());
        multiTenantInput.setSelected(data.isMultiTenant());
        clientIdInput.setText(data.getClientId());
    }

    private void createUIComponents() {
        noteComponent = ComponentsKt.noteComponent(MessageBundle.message("dialog.identity.ad.register_app.description"));
        noteComponent.setBorder(JBUI.Borders.emptyBottom(5));

        callbackUrlsInput = new AzureEditableCallbackUrlsCombobox();
        clientIdInput = new AzureClientIdInput();
        advancedSettingsSeparator = new AzureHideableTitledSeparator();
    }
}
