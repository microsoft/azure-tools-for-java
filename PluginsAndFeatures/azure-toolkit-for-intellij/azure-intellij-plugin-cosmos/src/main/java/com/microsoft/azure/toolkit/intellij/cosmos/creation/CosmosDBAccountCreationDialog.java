/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountDraft;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosServiceSubscription;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class CosmosDBAccountCreationDialog extends AzureDialog<CosmosDBAccountDraft.Config> implements AzureForm<CosmosDBAccountDraft.Config> {
    private static final Pattern COSMOS_DB_ACCOUNT_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9\\-]{1,42}[a-z0-9]$");

    private JPanel pnlRoot;
    private JLabel lblSubscription;
    private SubscriptionComboBox cbSubscription;
    private JLabel lblResourceGroup;
    private ResourceGroupComboBox cbResourceGroup;
    private JLabel lblName;
    private AzureTextInput txtName;
    private JLabel lblRegion;
    private RegionComboBox cbRegion;
    private AzureComboBox<DatabaseAccountKind> cbKind;
    private JLabel lblKind;

    private final Project project;

    public CosmosDBAccountCreationDialog(final Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.cbSubscription.setRequired(true);
        this.cbResourceGroup.setRequired(true);
        this.cbRegion.setRequired(true);
        this.txtName.setRequired(true);
        this.txtName.addValidator(this::validateCosmosDBAccountName);
        this.cbSubscription.addItemListener(this::onSubscriptionChanged);
        this.cbResourceGroup.addItemListener(e -> this.txtName.validateValueAsync()); // trigger validation after resource group changed

        this.lblSubscription.setLabelFor(cbSubscription);
        this.lblResourceGroup.setLabelFor(cbResourceGroup);
        this.lblName.setLabelFor(txtName);
        this.lblRegion.setLabelFor(cbRegion);
        this.lblKind.setLabelFor(cbKind);
    }

    private void onSubscriptionChanged(final ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) itemEvent.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.cbRegion.setSubscription(subscription);
            this.txtName.validateValueAsync(); // trigger validation after subscription changed
        }
    }

    private AzureValidationInfo validateCosmosDBAccountName() {
        final Subscription value = cbSubscription.getValue();
        if (value != null) {
            final CosmosServiceSubscription serviceSubscription = Azure.az(AzureCosmosService.class).forSubscription(value.getId());
            final Availability availability = serviceSubscription.checkNameAvailability(txtName.getValue());
            if (!availability.isAvailable()) {
                return AzureValidationInfo.error(availability.getUnavailabilityMessage(), txtName);
            }
        }
        return AzureValidationInfo.success(txtName);
    }

    @Override
    public AzureForm<CosmosDBAccountDraft.Config> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Cosmos DB Account";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public CosmosDBAccountDraft.Config getValue() {
        CosmosDBAccountDraft.Config result = new CosmosDBAccountDraft.Config();
        result.setSubscription(cbSubscription.getValue());
        result.setResourceGroup(cbResourceGroup.getValue());
        result.setName(txtName.getValue());
        result.setRegion(cbRegion.getValue());
        result.setKind(cbKind.getValue());
        return result;
    }

    @Override
    public void setValue(@Nonnull CosmosDBAccountDraft.Config data) {
        Optional.ofNullable(data.getSubscription()).ifPresent(cbSubscription::setValue);
        Optional.ofNullable(data.getResourceGroup()).ifPresent(cbResourceGroup::setValue);
        Optional.ofNullable(data.getName()).ifPresent(txtName::setValue);
        Optional.ofNullable(data.getRegion()).ifPresent(cbRegion::setValue);
        Optional.ofNullable(data.getKind()).ifPresent(cbKind::setValue);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, cbSubscription, cbResourceGroup, cbRegion, cbKind);
    }

    private void createUIComponents() {
        this.cbKind = new AzureComboBox<>(DatabaseAccountKind::values, true) {
            @Nonnull
            @Override
            protected List<ExtendableTextComponent.Extension> getExtensions() {
                return Collections.emptyList();
            }
        };
        this.cbRegion = new RegionComboBox() {
            @Nonnull
            @Override
            protected List<? extends Region> loadItems() {
                return Objects.isNull(this.subscription) ? Collections.emptyList() :
                        Azure.az(AzureCosmosService.class).forSubscription(this.subscription.getId()).listSupportedRegions();
            }
        };
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
