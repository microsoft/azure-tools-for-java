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
