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

package com.microsoft.intellij.util;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.CheckNameResourceTypes;
import com.microsoft.azure.management.appservice.implementation.ResourceNameAvailabilityInner;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.rest.RestException;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

public class ValidationUtils {
    private static final String PACKAGE_NAME_REGEX = "[a-zA-Z]([\\.a-zA-Z0-9_])*";
    private static final String GROUP_ARTIFACT_ID_REGEX = "[0-9a-zA-Z]([\\.a-zA-Z0-9\\-_])*";
    private static final String VERSION_REGEX = "[0-9]([\\.a-zA-Z0-9\\-_])*";
    private static final String AZURE_FUNCTION_NAME_REGEX = "[a-zA-Z]([a-zA-Z0-9\\-_])*";
    private static final String APP_SERVICE_PLAN_NAME_PATTERN = "[a-zA-Z0-9\\-]{1,40}";

    public static boolean isValidJavaPackageName(String packageName) {
        return packageName != null && packageName.matches(PACKAGE_NAME_REGEX);
    }

    public static boolean isValidGroupIdArtifactId(String name) {
        return name != null && name.matches(GROUP_ARTIFACT_ID_REGEX);
    }

    public static boolean isValidFunctionName(String name) {
        return name != null && name.matches(AZURE_FUNCTION_NAME_REGEX);
    }

    public static boolean isValidVersion(String version) {
        return version != null && version.matches(VERSION_REGEX);
    }

    public static void validateFunctionAppName(String subscriptionId, String functionAppName) {
        if (StringUtils.isEmpty(subscriptionId)) {
            throw new IllegalArgumentException("Subscription can not be null");
        }
        if (!isValidFunctionName(functionAppName)) {
            throw new IllegalArgumentException("Function App names only allow alphanumeric characters, periods, " +
                    "underscores, hyphens and parenthesis and cannot end in a period.");
        }
        try {
            final Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(subscriptionId);
            final ResourceNameAvailabilityInner result = azure.appServices().inner()
                    .checkNameAvailability(functionAppName, CheckNameResourceTypes.MICROSOFT_WEBSITES);
            if (!result.nameAvailable()) {
                throw new IllegalArgumentException(result.message());
            }
        } catch (IOException e) {
            // swallow exception when get azure client
        }
    }

    public static void validateResourceGroupName(String subscriptionId, String resourceGroup) {
        if (StringUtils.isEmpty(subscriptionId)) {
            throw new IllegalArgumentException("Subscription can not be null");
        }
        if (StringUtils.isEmpty(resourceGroup)) {
            throw new IllegalArgumentException("Resource group name can not be null");
        }
        try {
            final Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(subscriptionId);
            if (azure.resourceGroups().getByName(resourceGroup) != null) {
                throw new IllegalArgumentException("A resource group with the same name already exists in the selected subscription.");
            }
        } catch (RestException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IOException e) {
            // swallow exception when get azure client
        }
    }

    public static void validateAppServicePlanName(String appServicePlan){
        if (StringUtils.isEmpty(appServicePlan)) {
            throw new IllegalArgumentException("App Service Plan name is required");
        } else if (!appServicePlan.matches(APP_SERVICE_PLAN_NAME_PATTERN)) {
            throw new IllegalArgumentException(String.format("App Service Plan Name should match %s", APP_SERVICE_PLAN_NAME_PATTERN));
        }
    }
}
