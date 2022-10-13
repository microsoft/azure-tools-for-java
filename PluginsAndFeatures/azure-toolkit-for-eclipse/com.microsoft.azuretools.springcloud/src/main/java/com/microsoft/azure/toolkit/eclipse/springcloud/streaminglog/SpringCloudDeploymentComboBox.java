/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.springcloud.streaminglog;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;

public class SpringCloudDeploymentComboBox extends AzureComboBox<SpringCloudAppInstance> {

    private SpringCloudApp app;

    public SpringCloudDeploymentComboBox(Composite parent) {
        super(parent, false);
    }

    public void setSpringCloudApp(final SpringCloudApp app) {
        if (Objects.equals(app, this.app)) {
            return;
        }
        this.app = app;
        if (app == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @Override
    protected List<? extends SpringCloudAppInstance> loadItems() {
        return Optional.ofNullable(app).map(SpringCloudApp::getActiveDeployment)
                .map(deployment -> deployment.getInstances()).orElse(Collections.emptyList());
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof SpringCloudAppInstance
                ? ((SpringCloudAppInstance) item).name()
                : super.getItemText(item);
    }

}
