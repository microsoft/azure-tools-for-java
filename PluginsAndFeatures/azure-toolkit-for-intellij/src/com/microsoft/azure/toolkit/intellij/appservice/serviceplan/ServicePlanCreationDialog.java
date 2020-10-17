package com.microsoft.azure.toolkit.intellij.appservice.serviceplan;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.appservice.ServicePlanMock;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.AzureValidationInfoBuilder;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.util.ValidationUtils;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class ServicePlanCreationDialog extends AzureDialog<ServicePlanMock>
    implements AzureForm<ServicePlanMock> {
    public static final String DESCRIPTION =
        "App Service plan pricing tier determines the location, features, cost and compute resources associated with your app.";
    public static final String DIALOG_TITLE = "New App Service Plan";
    private final Subscription subscription;
    private final OperatingSystem os;
    private final Region region;
    private JPanel contentPanel;
    private JLabel labelDescription;
    private ValidationDebouncedTextInput textName;
    private PricingTierComboBox comboBoxPricingTier;

    public ServicePlanCreationDialog(final Subscription subscription, OperatingSystem os, Region region) {
        super();
        this.init();
        this.subscription = subscription;
        this.os = os;
        this.region = region;
        this.textName.setValidator(this::validateName);
    }

    private AzureValidationInfo validateName() {
        try {
            ValidationUtils.validateAppServicePlanName(this.textName.getValue());
        } catch (final IllegalArgumentException e) {
            final AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this.textName).type(AzureValidationInfo.Type.ERROR).message(e.getMessage()).build();
        }
        return AzureValidationInfo.OK;
    }

    @Override
    public AzureForm<ServicePlanMock> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return DIALOG_TITLE;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public ServicePlanMock getData() {
        final ServicePlanMock.ServicePlanMockBuilder builder = ServicePlanMock.builder();
        builder.subscription(this.subscription)
               .name(this.textName.getValue())
               .os(this.os)
               .region(this.region)
               .tier(this.comboBoxPricingTier.getValue());
        return builder.build();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }

    private void createUIComponents() {
        this.labelDescription = new JLabel("<html><body><b>" + DESCRIPTION + "</b></body></html");
    }
}
