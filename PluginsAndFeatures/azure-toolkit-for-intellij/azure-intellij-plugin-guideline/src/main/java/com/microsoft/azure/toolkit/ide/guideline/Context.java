/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Context {
    public Map<String, Object> parameters = new HashMap();

    public Object getProperty(String key) {
        return parameters.get(key);
    }

    public void setProperty(String key, Object value) {
        parameters.put(key, value);
    }
}
