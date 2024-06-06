package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.intellij.icons.AllIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureIntegerInput;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.StorageAuthenticationMethod;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;

public class FlexConsumptionConfigurationPanel implements AzureFormPanel<FlexConsumptionConfiguration> {
    public static final String IDENTITY_ID_VALIDATION_MESSAGE = "Identity Id is required for user assigned identity authentication";
    public static final String CONNECTION_KEY_VALIDATION_MESSAGE = "Connection Key is required for storage account connection string authentication";
    private JLabel lblInstanceMemory;
    private JRadioButton rdo512;
    private JRadioButton rdo2048;
    private JRadioButton rdo4096;
    private JLabel lblMaxInstances;
    private AzureIntegerInput txtMaxInstances;
    private JLabel lblHttpConcurrency;
    private AzureIntegerInput txtHttpConcurrency;
    private JLabel lblAuthMethod;
    private AzureComboBox<StorageAuthenticationMethod> cbAuthMethod;
    private JLabel lblIdentityId;
    private AzureTextInput txtIdentityId;
    private JLabel lblConnectionKey;
    private AzureTextInput txtConnectionKey;
    private JPanel pnlRoot;

    public FlexConsumptionConfigurationPanel() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        final ButtonGroup group = new ButtonGroup();
        group.add(rdo512);
        group.add(rdo2048);
        group.add(rdo4096);

        this.cbAuthMethod.addItemListener(this::onSelectAuthMethod);
        this.txtHttpConcurrency.setMinValue(1);
        this.txtHttpConcurrency.setMaxValue(1000);
        this.txtMaxInstances.setMinValue(40);
        this.txtMaxInstances.setMaxValue(1000);

        this.lblAuthMethod.setIcon(AllIcons.General.ContextHelp);
        this.lblIdentityId.setIcon(AllIcons.General.ContextHelp);
        this.lblConnectionKey.setIcon(AllIcons.General.ContextHelp);
        this.lblHttpConcurrency.setIcon(AllIcons.General.ContextHelp);
        this.lblMaxInstances.setIcon(AllIcons.General.ContextHelp);
        this.lblInstanceMemory.setIcon(AllIcons.General.ContextHelp);

        this.lblMaxInstances.setLabelFor(txtMaxInstances);
        this.txtMaxInstances.setRequired(true);
        this.cbAuthMethod.setRequired(true);
    }

    private void onSelectAuthMethod(ItemEvent itemEvent) {
        final StorageAuthenticationMethod method = this.cbAuthMethod.getValue();
        this.lblIdentityId.setVisible(method == StorageAuthenticationMethod.UserAssignedIdentity);
        this.txtIdentityId.setVisible(method == StorageAuthenticationMethod.UserAssignedIdentity);
        this.lblConnectionKey.setVisible(method == StorageAuthenticationMethod.StorageAccountConnectionString);
        this.txtConnectionKey.setVisible(method == StorageAuthenticationMethod.StorageAccountConnectionString);
    }

    @Override
    public void setValue(FlexConsumptionConfiguration data) {
        this.rdo512.setSelected(data.getInstanceSize() == 512);
        this.rdo2048.setSelected(data.getInstanceSize() == 2048);
        this.rdo4096.setSelected(data.getInstanceSize() == 4096);
        this.txtMaxInstances.setValue(data.getMaximumInstances());
        this.txtHttpConcurrency.setValue(data.getHttpInstanceConcurrency());
        final StorageAuthenticationMethod method = data.getAuthenticationMethod();
        this.cbAuthMethod.setValue(method);
        if (method == StorageAuthenticationMethod.StorageAccountConnectionString) {
            this.txtConnectionKey.setValue(data.getStorageAccountConnectionString());
        } else if (method == StorageAuthenticationMethod.UserAssignedIdentity) {
            this.txtIdentityId.setValue(data.getUserAssignedIdentityResourceId());
        }
    }

    @Nullable
    @Override
    public FlexConsumptionConfiguration getValue() {
        final FlexConsumptionConfiguration result = new FlexConsumptionConfiguration();
        result.setInstanceSize(this.rdo512.isSelected() ? 512 : this.rdo2048.isSelected() ? 2048 : 4096);
        result.setAuthenticationMethod(this.cbAuthMethod.getValue());
        if (StringUtils.isNotBlank(this.txtMaxInstances.getText())) {
            result.setMaximumInstances(this.txtMaxInstances.getValue());
        }
        if (StringUtils.isNotBlank(this.txtHttpConcurrency.getText())) {
            result.setHttpInstanceConcurrency(this.txtHttpConcurrency.getValue());
        }
        if (result.getAuthenticationMethod() == StorageAuthenticationMethod.StorageAccountConnectionString) {
            result.setStorageAccountConnectionString(this.txtConnectionKey.getValue());
        } else if (result.getAuthenticationMethod() == StorageAuthenticationMethod.UserAssignedIdentity) {
            result.setUserAssignedIdentityResourceId(this.txtIdentityId.getValue());
        }
        return result;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(this.txtMaxInstances, this.txtHttpConcurrency, this.cbAuthMethod, this.txtConnectionKey, this.txtIdentityId);
    }

    @Override
    public List<AzureValidationInfo> validateAdditionalInfo() {
        final StorageAuthenticationMethod value = cbAuthMethod.getValue();
        if (value == StorageAuthenticationMethod.UserAssignedIdentity && StringUtils.isBlank(txtIdentityId.getText())) {
            return List.of(AzureValidationInfo.error(IDENTITY_ID_VALIDATION_MESSAGE, txtIdentityId));
        } else if (value == StorageAuthenticationMethod.StorageAccountConnectionString && StringUtils.isBlank(txtConnectionKey.getText())) {
            return List.of(AzureValidationInfo.error(CONNECTION_KEY_VALIDATION_MESSAGE, txtConnectionKey));
        }
        return AzureFormPanel.super.validateAdditionalInfo();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbAuthMethod = new AzureComboBox<>(() -> List.of(StorageAuthenticationMethod.values()));
    }
}
