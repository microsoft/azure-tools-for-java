/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudAppInstanceComboBox extends AzureComboBox<SpringCloudAppInstance> {
    private SpringCloudApp app;
    @Override
    protected String getItemText(Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        return ((SpringCloudAppInstance) item).getName();
    }

    public void setApp(SpringCloudApp app) {
        if (Objects.equals(app, this.app)) {
            return;
        }
        this.app = app;
        if (Objects.isNull(app)) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Nonnull
    @Override
    protected List<? extends SpringCloudAppInstance> loadItems() {
        final List<SpringCloudAppInstance> appInstanceList = new ArrayList<>();
        Optional.ofNullable(this.app).flatMap(springCloudApp -> Optional.ofNullable(springCloudApp.getActiveDeployment())).ifPresent(deployment -> appInstanceList.addAll(deployment.getInstanceResources()));
        return appInstanceList;
    }

    @Override
    public void setValue(SpringCloudAppInstance val) {
        super.setValue(val);
    }
}
