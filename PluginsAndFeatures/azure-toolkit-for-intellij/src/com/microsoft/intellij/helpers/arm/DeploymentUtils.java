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
package com.microsoft.intellij.helpers.arm;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azuretools.azurecommons.util.Utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class DeploymentUtils {

    private static final String EMPTY_PARAMETER = "{}";
    private static final String[] VALID_PARAMETER_ATTRIBUTES = {"value", "reference", ",metadata"};

    public static String serializeParameters(Deployment deployment) {
        Object parameterObject = deployment.parameters();
        if (!(parameterObject != null && parameterObject instanceof Map)) {
            return EMPTY_PARAMETER;
        }
        Map<String, Map<String, String>> parameters = (Map<String, Map<String, String>>) deployment.parameters();
        // Remove extra attributes in parameters
        // Refers https://schema.management.azure.com/schemas/2015-01-01/deploymentParameters.json#
        parameters.values().forEach(value -> {
            Iterator<Map.Entry<String, String>> iterator = value.entrySet().iterator();
            while (iterator.hasNext()) {
                final String parameterKey = iterator.next().getKey();
                if (!Arrays.stream(VALID_PARAMETER_ATTRIBUTES)
                        .anyMatch(attribute -> attribute.equals(parameterKey))) {
                    iterator.remove();
                }
            }
        });
        return Utils.getPrettyJson(new Gson().toJson(parameters));
    }

    public static String parseParameters(String parameters) {
        Gson gson = new Gson();
        JsonElement parametersElement = gson.fromJson(parameters, JsonElement.class).getAsJsonObject().get("parameters");
        return parametersElement == null ? parameters : parametersElement.toString();
    }
}
