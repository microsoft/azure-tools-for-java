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
 *
 */

package com.microsoft.intellij.runner.functions;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class AzureFunctionsConstants {

    public static final String FUNCTION_JAVA_LIBRARY_ARTIFACT_ID = "azure-functions-java-library";
    public static final String DISPLAY_NAME = "Azure Functions";
    public static final String AZURE_FUNCTIONS_ICON = "azure-functions-small.png";
    public static final String LOADING_TEXT = "Loading...";
    public static final String EMPTY_TEXT = "Empty";
    public static final String NEED_SIGN_IN = "Please sign in with your Azure account.";
    public static final String NEW_CREATED_RESOURCE = "%s (New Created)";

    public static final Map<String, String> HINT = new HashMap<String, String>() {{
            put("AzureWebJobsStorage", "The Azure Functions runtime uses this storage account connection " +
                    "string for all functions except for HTTP triggered functions.");
            put("FUNCTIONS_WORKER_RUNTIME", "The language worker runtime to load in the function app.");
        }};

    public static String getAppSettingHint(String appSettingKey) {
        return HINT.containsKey(appSettingKey) ? HINT.get(appSettingKey) : StringUtils.EMPTY;
    }
}
