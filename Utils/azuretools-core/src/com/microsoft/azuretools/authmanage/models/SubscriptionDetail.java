/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.models;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionDetail {
    @JsonProperty
    private String subscriptionId;
    @JsonProperty
    private String subscriptionName;
    @JsonProperty
    private String tenantId;
    @JsonProperty
    private boolean selected;

    // for json mapper
    @SuppressWarnings("unused")
    private SubscriptionDetail(){}

    public SubscriptionDetail(String subscriptionId, String subscriptionName, String tenantId, boolean selected) {
        this.subscriptionId = subscriptionId;
        this.subscriptionName = subscriptionName;
        this.tenantId = tenantId;
        this.selected = selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isSelected() {
        return this.selected;
    }

    @Override
    public String toString() {
        return subscriptionName;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SubscriptionDetail)) {
            return false;
        }
        SubscriptionDetail other =  (SubscriptionDetail)obj;
        return (this.subscriptionId == null)
                ? other.subscriptionId == null
                : this.subscriptionId.toLowerCase().equals(other.subscriptionId.toLowerCase());
    }

    @Override
    public int hashCode() {
        return this.subscriptionId.toLowerCase().hashCode();
    }
}
