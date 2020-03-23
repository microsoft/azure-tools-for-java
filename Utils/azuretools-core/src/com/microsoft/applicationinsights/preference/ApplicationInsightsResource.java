/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.applicationinsights.preference;

import java.io.Serializable;
/**
 * Model class to store application insights resource data.
 */
public class ApplicationInsightsResource implements Serializable, Comparable<ApplicationInsightsResource> {
    private static final long serialVersionUID = -5687551495552719808L;
    String resourceName;
    String instrumentationKey;
    String subscriptionName;
    String subscriptionId;
    String location;
    String resourceGroup;
    // imported = false --> manually added without authentication
    boolean imported;

    public ApplicationInsightsResource() {
        super();
    }

    public ApplicationInsightsResource(String resourceName,
            String instrumentationKey, String subscriptionName,
            String subscriptionId, String location, String resourceGroup,
            boolean imported) {
        super();
        this.resourceName = resourceName;
        this.instrumentationKey = instrumentationKey;
        this.subscriptionName = subscriptionName;
        this.subscriptionId = subscriptionId;
        this.location = location;
        this.resourceGroup = resourceGroup;
        this.imported = imported;
    }

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

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ApplicationInsightsResource resource = (ApplicationInsightsResource) obj;
        String key = resource.getInstrumentationKey();
        boolean value = instrumentationKey != null && instrumentationKey.equals(key);
        return value;
    }

    @Override
    public int compareTo(ApplicationInsightsResource object) {
        int value = resourceName.compareToIgnoreCase(object.getResourceName());
        return value;
    }
}
