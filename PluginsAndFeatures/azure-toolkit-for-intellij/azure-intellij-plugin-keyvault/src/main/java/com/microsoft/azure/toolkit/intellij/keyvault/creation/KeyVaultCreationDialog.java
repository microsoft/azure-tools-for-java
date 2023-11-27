/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.creation;

import com.azure.resourcemanager.keyvault.models.SkuName;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.keyvault.AzureKeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVaultDraft;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.*;

public class KeyVaultCreationDialog extends AzureDialog<KeyVaultDraft.Config> implements AzureFormPanel<KeyVaultDraft.Config> {
    public static final String RBAC_DOCUMENT = "Azure RBAC is an authorization system built on Azure Resource Manager that provides fine-grained access management to Azure resources," +
            " <a href = \"https://learn.microsoft.com/en-us/azure/role-based-access-control/overview\">please refer this document for more details</a>.";
    public static final String VAULT_ACCESS_DOCUMENT = "A Key Vault access policy determines whether a given security principal, namely a user, application or user group, can perform different operations on Key Vault secrets, keys, and certificates," +
            " <a href = \"https://learn.microsoft.com/en-us/azure/key-vault/general/assign-access-policy\">please refer this document for more details</a>.";
    private JLabel lblSubscription;
    private JLabel lblResourceGroup;
    private ResourceGroupComboBox cbResourceGroup;
    private JLabel lblName;
    private AzureTextInput txtName;
    private JLabel lblRegion;
    private SubscriptionComboBox selectorSubscription;
    private RegionComboBox selectorRegion;
    private AzureComboBox<SkuName> cbSku;
    private JLabel lblSku;
    private JPanel pnlRoot;
    private JRadioButton rdoRBAC;
    private JRadioButton rdoVaultAccess;
    private JBLabel lblPermissionDoc;

    private Project project;

    public KeyVaultCreationDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$();
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(rdoRBAC);
        buttonGroup.add(rdoVaultAccess);
        this.txtName.setRequired(true);
        this.txtName.addValidator(this::validateVaultName);
        this.selectorSubscription.setRequired(true);
        this.selectorRegion.setRequired(true);
        this.cbSku.setRequired(true);
        this.cbResourceGroup.setRequired(true);
        lblName.setIcon(AllIcons.General.ContextHelp);
        lblPermissionDoc.setText(RBAC_DOCUMENT);
        rdoRBAC.addActionListener(ignore -> lblPermissionDoc.setText(RBAC_DOCUMENT));
        rdoVaultAccess.addActionListener(ignore -> lblPermissionDoc.setText(VAULT_ACCESS_DOCUMENT));

        this.lblSubscription.setLabelFor(this.selectorSubscription);
        this.lblResourceGroup.setLabelFor(this.cbResourceGroup);
        this.lblName.setLabelFor(this.txtName);
        this.lblRegion.setLabelFor(this.selectorRegion);
        this.lblSku.setLabelFor(this.cbSku);
        this.selectorSubscription.addItemListener(this::onSelectSubscription);
    }

    private AzureValidationInfo validateVaultName() {
        final Subscription subscription = this.selectorSubscription.getValue();
        if (Objects.isNull(subscription)) {
            return AzureValidationInfo.none(txtName);
        }
        final String name = this.txtName.getValue();
        final Availability availability = Azure.az(AzureKeyVault.class).keyVaults(subscription.getId()).checkNameAvailability(name);
        return availability.isAvailable() ? AzureValidationInfo.success(txtName) :
                AzureValidationInfo.error(availability.getUnavailabilityReason(), txtName);
    }

    private void onSelectSubscription(@Nonnull ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) e.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.selectorRegion.setSubscription(subscription);
        }
    }

    @Override
    public AzureForm<KeyVaultDraft.Config> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return "Create Key Vault";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public void setValue(KeyVaultDraft.Config data) {
        Optional.ofNullable(data.getName()).ifPresent(txtName::setValue);
        Optional.ofNullable(data.getSubscription()).ifPresent(selectorSubscription::setValue);
        Optional.ofNullable(data.getResourceGroup()).ifPresent(cbResourceGroup::setValue);
        Optional.ofNullable(data.getRegion()).ifPresent(selectorRegion::setValue);
        Optional.ofNullable(data.getSku()).ifPresent(cbSku::setValue);
        Optional.ofNullable(data.isUseAzureRBAC()).ifPresent(rdoRBAC::setSelected);
        Optional.ofNullable(data.isUseAzureRBAC()).ifPresent(result -> rdoVaultAccess.setSelected(!result));
    }

    @Override
    public KeyVaultDraft.Config getValue() {
        final KeyVaultDraft.Config result = new KeyVaultDraft.Config();
        result.setSubscription(selectorSubscription.getValue());
        result.setResourceGroup(cbResourceGroup.getValue());
        result.setName(txtName.getValue());
        result.setRegion(selectorRegion.getValue());
        result.setSku(cbSku.getValue());
        result.setUseAzureRBAC(rdoRBAC.isSelected());
        return result;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, selectorSubscription, cbResourceGroup, selectorRegion, cbSku);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbSku = new AzureComboBox<SkuName>(() -> Arrays.asList(SkuName.STANDARD, SkuName.PREMIUM)) {
            @Override
            protected String getItemText(final Object item) {
                return item instanceof SkuName ? StringUtils.capitalize(StringUtils.lowerCase(((SkuName) item).name())) : super.getItemText(item);
            }

            @Nonnull
            @Override
            protected List<ExtendableTextComponent.Extension> getExtensions() {
                return Collections.emptyList();
            }
        };

        this.lblPermissionDoc = new JBLabel();
        this.lblPermissionDoc.setFont(JBFont.regular());
        this.lblPermissionDoc.setAllowAutoWrapping(true);
        this.lblPermissionDoc.setCopyable(true);// this makes label auto wrapping
        this.lblPermissionDoc.setForeground(UIUtil.getContextHelpForeground());
    }
}
