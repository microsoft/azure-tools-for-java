/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.webapp;

import java.util.Map;

public class WebAppProperty {

    private Map<String, Object> propertyMap;

    public WebAppProperty(Map<String, Object> propertyMap) {
        this.propertyMap = propertyMap;
    }

    public Object getValue(String key) {
        return this.propertyMap.get(key);
    }

}
