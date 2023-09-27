/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.util.EnvironmentUtil;
import com.microsoft.azure.toolkit.lib.common.utils.ProcessEnvironmentProvider;

import java.util.Map;

public class IntelliJProcessEnvironmentProvider implements ProcessEnvironmentProvider {
    @Override
    public Map<String, String> getEnvironment() {
        return EnvironmentUtil.getEnvironmentMap();
    }
}
