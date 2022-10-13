/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.springcloud.streaminglog;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;

public class SpringCloudStreamingLogDialog extends AzureDialog<SpringCloudAppInstance> {

    private SpringCloudApp app;
    private SpringCloudLogStreamingComposite composite;

    public SpringCloudStreamingLogDialog(final Shell parentShell, final SpringCloudApp app) {
        super(parentShell);
        this.app = app;
    }

    @Override
    protected String getDialogTitle() {
        return "Select Instance for log Streaming";
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        composite = new SpringCloudLogStreamingComposite(container, SWT.NONE);
        GridData gdComposite = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        composite.setLayoutData(gdComposite);
        composite.setSpringCloudApp(app);
        return container;
    }

    @Override
    public AzureForm<SpringCloudAppInstance> getForm() {
        return composite;
    }

}
