/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.preference;

import java.util.ArrayList;
import java.util.List;

public class ApplicationInsightsResourceWithSameName {
    String resourceName;
    List<Integer> indices = new ArrayList<Integer>();

    public ApplicationInsightsResourceWithSameName() {
        super();
    }

    public ApplicationInsightsResourceWithSameName(String resourceName) {
        super();
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
    public List<Integer> getIndices() {
        return indices;
    }
    public void setIndices(List<Integer> indices) {
        this.indices = indices;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ApplicationInsightsResourceWithSameName resource =
                (ApplicationInsightsResourceWithSameName) obj;
        return (resourceName != null
                && resourceName.equals(resource.getResourceName()));
    }
}
