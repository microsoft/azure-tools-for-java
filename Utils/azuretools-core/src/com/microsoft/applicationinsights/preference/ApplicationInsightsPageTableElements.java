/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.preference;

import java.util.List;

public class ApplicationInsightsPageTableElements {
    private List<ApplicationInsightsPageTableElement> elements;

    public List<ApplicationInsightsPageTableElement> getElements() {
        return elements;
    }

    public void setElements(List<ApplicationInsightsPageTableElement> elements) {
        this.elements = elements;
    }
}
