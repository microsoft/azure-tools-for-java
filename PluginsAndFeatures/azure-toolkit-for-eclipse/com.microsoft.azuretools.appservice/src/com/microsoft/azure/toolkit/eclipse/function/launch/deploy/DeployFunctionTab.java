/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.launch.deploy;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureTextInput;
import com.microsoft.azure.toolkit.eclipse.function.launch.LaunchConfigurationUtils;
import com.microsoft.azure.toolkit.eclipse.function.model.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.eclipse.function.ui.FunctionProjectComboBox;
import com.microsoft.azure.toolkit.eclipse.function.utils.FunctionUtils;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.lang.StringUtils;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DeployFunctionTab extends AbstractLaunchConfigurationTab implements AzureForm<FunctionDeployConfiguration> {
    private FunctionProjectComboBox cbProject;
    private AzureTextInput txtFunctionCli;

    @Override
    public void createControl(Composite parent) {

        Composite comp = new Group(parent, SWT.BORDER);
        setControl(comp);

        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(comp);
        comp.setLayout(new GridLayout(2, false));

        Label lblProject = new Label(comp, SWT.NONE);
        lblProject.setText("Project:");
        GridDataFactory.swtDefaults().applyTo(lblProject);

        cbProject = new FunctionProjectComboBox(comp);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(cbProject);

        Label lblNewLabel = new Label(comp, SWT.NONE);
        lblNewLabel.setText("Function CLI:");

        txtFunctionCli = new AzureTextInput(comp);
        txtFunctionCli.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblLocalSettings = new Label(comp, SWT.NONE);
        lblLocalSettings.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblLocalSettings.setText("Local settings:");

        // to start getValue -> compare with origin model -> mark dirty -> Enable apple
        this.cbProject.addValueChangedListener(v -> {
            markDirty();
        });

        this.txtFunctionCli.addValueChangedListener(v -> {
            markDirty();
        });
    }


    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        FunctionDeployConfiguration origin = LaunchConfigurationUtils.getFromConfiguration(configuration, FunctionDeployConfiguration.class);
        setValue(origin);
        this.cbProject.refreshItems();
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        FunctionDeployConfiguration origin = LaunchConfigurationUtils.getFromConfiguration(configuration, FunctionDeployConfiguration.class);
        FunctionDeployConfiguration newConfig = getValue();
        try {

            Utils.mergeObjects(newConfig, origin);
        } catch (IllegalAccessException e) {
            // ignore
        }
        LaunchConfigurationUtils.saveToConfiguration(newConfig, configuration);
    }

    private void markDirty() {
        if (!this.getControl().isDisposed()) {
            setDirty(true);
            updateLaunchConfigurationDialog();
        }
    }

    public FunctionDeployConfiguration getValue() {
        FunctionDeployConfiguration config = new FunctionDeployConfiguration();
        if (cbProject.getValue() != null) {
            config.setProjectName(cbProject.getValue().getElementName());
        }
        if (StringUtils.isNotBlank(txtFunctionCli.getValue())) {
            config.setFunctionCliPath(txtFunctionCli.getValue());
        }
        return config;
    }

    @Override
    public void setValue(FunctionDeployConfiguration config) {
        if (StringUtils.isNotBlank(config.getProjectName())) {
            this.cbProject.setValue(new AzureComboBox.ItemReference<>(config.getProjectName(), IJavaElement::getElementName));
        }
        if (StringUtils.isNotBlank(config.getFunctionCliPath())) {
            txtFunctionCli.setText(config.getFunctionCliPath());
        } else {
            try {
                txtFunctionCli.setText(FunctionUtils.getFuncPath());
            } catch (IOException | InterruptedException e) {
                AzureMessager.getMessager().warning("Cannot find function core tools due to error:" + e.getMessage());
            }
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(this.cbProject, this.txtFunctionCli);
    }

    @Override
    public String getName() {
        return "Deploy Functions tab";
    }

}
