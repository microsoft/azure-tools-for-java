package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.ComponentsKt;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class RegisterAzureApplicationForm implements AzureFormJPanel<ApplicationRegistrationModel> {
    private JPanel contentPanel;

    private AzureTextInput displayNameInput;
    private AzureTextInput callbackUrlInput;
    private AzureTextInput domainInput;
    private JBCheckBox multiTenantInput;
    private AzureTextInput clientIdInput;
    private JBCheckBox allowOverwriteInput;
    private JPanel advancedSettingsContentPanel;
    private TitledSeparator advancedSettingsSeparator;
    private JComponent noteComponent;

    // called by the managing component
    void init() {
        AzureHideableTitledSeparator separator = (AzureHideableTitledSeparator) advancedSettingsSeparator;
        separator.addContentComponent(advancedSettingsContentPanel);
        separator.collapse();
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
        data.setCallbackUrl(callbackUrlInput.getText());
        data.setDomain(domainInput.getText());
        data.setMultiTenant(multiTenantInput.isSelected());
        data.setClientId(clientIdInput.getText());
        data.setAllowOverwrite(allowOverwriteInput.isSelected());
        return data;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(displayNameInput, callbackUrlInput, domainInput, clientIdInput);
    }

    @Override
    public void setData(ApplicationRegistrationModel data) {
        displayNameInput.setText(data.getDisplayName());
        callbackUrlInput.setText(data.getCallbackUrl());
        domainInput.setText(data.getDomain());
        multiTenantInput.setSelected(data.isMultiTenant());
        clientIdInput.setText(data.getClientId());
        allowOverwriteInput.setSelected(data.getAllowOverwrite());
    }

    private void createUIComponents() {
        noteComponent = ComponentsKt.noteComponent(MessageBundle.message("dialog.identity.ad.register_app.description"));
        noteComponent.setBorder(JBUI.Borders.emptyBottom(5));

        callbackUrlInput = new AzureCallbackURLInput();
        clientIdInput = new AzureClientIdInput();
        advancedSettingsSeparator = new AzureHideableTitledSeparator();
    }
}
