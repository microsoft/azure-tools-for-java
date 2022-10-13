/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.springcloud.streaminglog;

import java.util.Arrays;
import java.util.List;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;

public class SpringCloudLogStreamingComposite extends Composite implements AzureForm<SpringCloudAppInstance> {

    private SpringCloudDeploymentComboBox springCloudDeploymentComboBox;

    public SpringCloudLogStreamingComposite(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(2, false));

        Label lblInstance = new Label(this, SWT.NONE);
        lblInstance.setText("Instance:");

        springCloudDeploymentComboBox = new SpringCloudDeploymentComboBox(this);
        springCloudDeploymentComboBox.setLabeledBy(lblInstance);
        springCloudDeploymentComboBox.setRequired(true);
        springCloudDeploymentComboBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(springCloudDeploymentComboBox);
    }

    public void setSpringCloudApp(final SpringCloudApp app) {
        springCloudDeploymentComboBox.setSpringCloudApp(app);
    }

    @Override
    public SpringCloudAppInstance getValue() {
        return springCloudDeploymentComboBox.getValue();
    }

    @Override
    public void setValue(SpringCloudAppInstance value) {
        springCloudDeploymentComboBox.setValue(value);
    }

}
