/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.guidance;

import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.containerapps.creation.ContainerAppCreationDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

public class ContainerAppNameInput implements AzureFormJPanel<String> {
    private JPanel pnlRoot;
    private AzureTextInput txtAppName;

    @Setter
    private ContainerAppsEnvironment env;

    public ContainerAppNameInput() {
        $$$setupUI$$$();
        this.txtAppName.setRequired(true);
        this.txtAppName.setLabel("App Name");
        this.txtAppName.addValidator(() -> {
            try {
                return validateContainerAppName(txtAppName.getValue(), this.env);
            } catch (final IllegalArgumentException e) {
                final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(txtAppName).type(AzureValidationInfo.Type.ERROR).message(e.getMessage()).build();
            }
        });
    }

    @Override
    public String getValue() {
        return txtAppName.getValue();
    }

    public void setValue(final String value) {
        this.txtAppName.setValue(value);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(txtAppName);
    }

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }

    private AzureValidationInfo validateContainerAppName(String name, ContainerAppsEnvironment env) {
        if (env != null && !env.isDraftForCreating()) {
            final Availability availability = env.checkContainerAppNameAvailability(name);
            return availability.isAvailable() ? AzureValidationInfo.success(this.txtAppName) :
                AzureValidationInfo.error(availability.getUnavailabilityMessage(), this.txtAppName);
        } else {
            final Matcher matcher = ContainerAppCreationDialog.CONTAINER_APP_NAME_PATTERN.matcher(name);
            return matcher.matches() && !StringUtils.contains(name, "--") ? AzureValidationInfo.success(this.txtAppName) :
                AzureValidationInfo.error(ContainerAppCreationDialog.CONTAINER_APP_NAME_VALIDATION_MESSAGE, this.txtAppName);
        }
    }
}
