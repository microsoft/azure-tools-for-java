/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureIntegerInput;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfile;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfileType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.*;

public class WorkloadProfileCreationDialog extends AzureDialog<WorkloadProfile> implements AzureForm<WorkloadProfile> {
    public static final WorkloadProfile DEFAULT_VALUE = WorkloadProfile.builder().minimumCount(3).maximumCount(5).build();
    public static final String PROFILE_NAME_REGEX = "^[a-zA-Z]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$";
    public static final String MINIUM_COUNT_LESS_WARNING = "Setting minimum instance count to less than 3 is not recommended for production workloads due to redundancy concerns.";
    public static final String INVALID_MAXIMUM_COUNT_ERROR = "Maximum count should be greater than or equal to minimum count.";
    private JLabel lblName;
    private AzureTextInput txtName;
    private AzureComboBox<WorkloadProfileType> cbSize;
    private JPanel pnlRoot;
    private AzureIntegerInput txtMinimumCount;
    private JLabel lblMaximumCount;
    private AzureIntegerInput txtMaximumCount;
    private JLabel lblMinimumCount;

    private final String subscriptionId;
    private final Region region;

    public WorkloadProfileCreationDialog(String subscriptionId, Region region) {
        super();
        this.subscriptionId = subscriptionId;
        this.region = region;
        $$$setupUI$$$();
        this.init();
    }

    @Override
    protected void init() {
        super.init();

        this.lblName.setLabelFor(txtName);
        this.txtName.setRequired(true);
        this.txtName.addValidator(this::validateProfileName);
        this.txtMinimumCount.setMinValue(0);
        this.txtMinimumCount.setMaxValue(20);
        this.lblMinimumCount.setLabelFor(txtMinimumCount);
        this.txtMaximumCount.setMinValue(0);
        this.txtMaximumCount.setMaxValue(20);
        this.lblMaximumCount.setLabelFor(txtMaximumCount);

        this.txtMinimumCount.addValidator(() -> {
            final Integer min = txtMinimumCount.getValue();
            if (Objects.nonNull(min) && min < 3) {
                return AzureValidationInfo.warning(MINIUM_COUNT_LESS_WARNING, txtMinimumCount);
            }
            return AzureValidationInfo.ok(txtMinimumCount);
        });
    }

    private AzureValidationInfo validateProfileName() {
        final String name = txtName.getValue();
        if (StringUtils.isBlank(name)) {
            return AzureValidationInfo.error("Name should not be empty.", txtName);
        }
        if (name.length() > 15) {
            return AzureValidationInfo.error("Workload profile name must be at most 15 characters.", txtName);
        }
        if (!name.matches(PROFILE_NAME_REGEX)) {
            return AzureValidationInfo.error("The workload profile name must begin with letters, contain only letters, numbers, underscores, or dashes, and not end with an underscore or dash.", txtName);
        }
        return AzureValidationInfo.ok(txtName);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbSize = new WorkloadProfileTypeComboBox(subscriptionId, region);
    }

    @Override
    public List<AzureValidationInfo> validateAdditionalInfo() {
        final List<AzureValidationInfo> result = new ArrayList<>();
        final Integer min = txtMinimumCount.getValue();
        final Integer max = txtMaximumCount.getValue();
        if (Objects.nonNull(max) && Objects.nonNull(min) && min > max) {
            result.add(AzureValidationInfo.error(INVALID_MAXIMUM_COUNT_ERROR, txtMaximumCount));
        }
        return result;
    }

    @Override
    public WorkloadProfile getValue() {
        return WorkloadProfile.builder()
                .name(txtName.getValue())
                .maximumCount(txtMaximumCount.getValue())
                .minimumCount(txtMinimumCount.getValue())
                .type(cbSize.getValue()).build();
    }

    @Override
    public void setValue(@Nonnull final WorkloadProfile data) {
        Optional.ofNullable(data.getName()).ifPresent(txtName::setValue);
        Optional.ofNullable(data.getWorkloadProfileType()).ifPresent(type ->
                cbSize.setValue(profile -> StringUtils.equalsIgnoreCase(profile.getName(), type)));
        Optional.ofNullable(data.getMaximumCount()).ifPresent(txtMaximumCount::setValue);
        Optional.ofNullable(data.getMinimumCount()).ifPresent(txtMinimumCount::setValue);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, cbSize, txtMaximumCount, txtMinimumCount);
    }

    @Override
    public AzureForm<WorkloadProfile> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return "New Workload Profile";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
