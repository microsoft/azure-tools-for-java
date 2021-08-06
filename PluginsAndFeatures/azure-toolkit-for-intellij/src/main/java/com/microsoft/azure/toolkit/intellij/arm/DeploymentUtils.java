/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
