/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureFunctionsConstants {
    public static final String DISPLAY_NAME = "Azure Functions";
    public static final String AZURE_FUNCTIONS_ICON = "azure-functions-small.png";

    public static final Map<String, String> HINT = new HashMap<String, String>() {{
            put("AzureWebJobsStorage", message("function.hint.azureWebJobsStorage"));
            put("FUNCTIONS_WORKER_RUNTIME", message("function.hint.functionsWorkerRuntime"));
        }};

    public static String getAppSettingHint(String appSettingKey) {
        return HINT.containsKey(appSettingKey) ? HINT.get(appSettingKey) : StringUtils.EMPTY;
    }
}
