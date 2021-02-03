/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.preference;
/**
 * Model class for data required to show on table.
 */
public class ApplicationInsightsPageTableElement {
    private String resourceName;
    private String instrumentationKey;

    public String getResourceName() {
        return resourceName;
    }
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
    public String getInstrumentationKey() {
        return instrumentationKey;
    }
    public void setInstrumentationKey(String instrumentationKey) {
        this.instrumentationKey = instrumentationKey;
    }
}
