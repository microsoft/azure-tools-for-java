/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.CheckNameResourceTypes;
import com.microsoft.azure.management.appservice.implementation.ResourceNameAvailabilityInner;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.rest.RestException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ValidationUtils {
    private static final String PACKAGE_NAME_REGEX = "[a-zA-Z]([\\.a-zA-Z0-9_])*";
    private static final String GROUP_ARTIFACT_ID_REGEX = "[0-9a-zA-Z]([\\.a-zA-Z0-9\\-_])*";
    private static final String VERSION_REGEX = "[0-9]([\\.a-zA-Z0-9\\-_])*";
    private static final String AZURE_FUNCTION_NAME_REGEX = "[a-zA-Z]([a-zA-Z0-9\\-_])*";
    private static final String APP_SERVICE_PLAN_NAME_PATTERN = "[a-zA-Z0-9\\-]{1,40}";
    //refer: https://dev.azure.com/msazure/AzureDMSS/_git/AzureDMSS-PortalExtension?path=%2Fsrc%2FSpringCloudPortalExt%2FClient%2FCreateApplication%2FCreateApplicationBlade.ts&version=GBdev&line=463&lineEnd=463&lineStartColumn=25&lineEndColumn=55&lineStyle=plain&_a=contents
    private static final String SPRING_CLOUD_APP_NAME_PATTERN = "^[a-z][a-z0-9-]{2,30}[a-z0-9]$";
    private static final String APP_INSIGHTS_NAME_INVALID_CHARACTERS = "[*;/?:@&=+$,<>#%\\\"\\{}|^'`\\\\\\[\\]]";

    private static Map<Pair<String, String>, String> appServiceNameValidationCache = new HashMap<>();
    private static Map<String, String> resourceGroupValidationCache = new HashMap<>();

    public static boolean isValidJavaPackageName(String packageName) {
        return packageName != null && packageName.matches(PACKAGE_NAME_REGEX);
    }

    public static boolean isValidGroupIdArtifactId(String name) {
        return name != null && name.matches(GROUP_ARTIFACT_ID_REGEX);
    }

    public static boolean isValidAppServiceName(String name) {
        return name != null && name.matches(AZURE_FUNCTION_NAME_REGEX);
    }

    public static boolean isValidSpringCloudAppName(String name) {
        int len = name.trim().length();
        return name != null && name.matches(SPRING_CLOUD_APP_NAME_PATTERN) && len >= 4 && len <= 32;
    }

    public static boolean isValidVersion(String version) {
        return version != null && version.matches(VERSION_REGEX);
    }

    public static void validateAppServiceName(String subscriptionId, String appServiceName) {
        final Pair<String, String> cacheKey = Pair.of(subscriptionId, appServiceName);
        if (appServiceNameValidationCache.containsKey(cacheKey)) {
            throwCachedValidationResult(appServiceNameValidationCache.get(cacheKey));
            return;
        }
        if (StringUtils.isEmpty(subscriptionId)) {
            cacheAndThrow(appServiceNameValidationCache, cacheKey, message("appService.subscription.validate.empty"));
        }
        if (!isValidAppServiceName(appServiceName)) {
            cacheAndThrow(appServiceNameValidationCache, cacheKey, message("appService.subscription.validate.invalidName"));
        }
        final Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(subscriptionId);
        final ResourceNameAvailabilityInner result = azure.appServices().inner()
            .checkNameAvailability(appServiceName, CheckNameResourceTypes.MICROSOFT_WEBSITES);
        if (!result.nameAvailable()) {
            cacheAndThrow(appServiceNameValidationCache, cacheKey, result.message());
        }
        appServiceNameValidationCache.put(cacheKey, null);
    }

    public static void validateResourceGroupName(String subscriptionId, String resourceGroup) {
        if (resourceGroupValidationCache.containsKey(subscriptionId)) {
            throwCachedValidationResult(appServiceNameValidationCache.get(subscriptionId));
            return;
        }
        if (StringUtils.isEmpty(subscriptionId)) {
            cacheAndThrow(resourceGroupValidationCache, subscriptionId, message("appService.subscription.validate.empty"));
        }
        if (StringUtils.isEmpty(resourceGroup)) {
            cacheAndThrow(resourceGroupValidationCache, subscriptionId, message("appService.resourceGroup.validate.empty"));
        }
        try {
            final Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(subscriptionId);
            if (azure.resourceGroups().getByName(resourceGroup) != null) {
                cacheAndThrow(resourceGroupValidationCache, subscriptionId, message("appService.resourceGroup.validate.exist"));
            }
        } catch (RestException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        resourceGroupValidationCache.put(subscriptionId, null);
    }

    public static void validateAppServicePlanName(String appServicePlan) {
        if (StringUtils.isEmpty(appServicePlan)) {
            throw new IllegalArgumentException(message("appService.servicePlan.validate.empty"));
        } else if (!appServicePlan.matches(APP_SERVICE_PLAN_NAME_PATTERN)) {
            throw new IllegalArgumentException(message("appService.servicePlan.validate.invalidName", APP_SERVICE_PLAN_NAME_PATTERN));
        }
    }

    public static void validateApplicationInsightsName(String applicationInsightsName) {
        if (StringUtils.isEmpty(applicationInsightsName)) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.empty"));
        }
        if (applicationInsightsName.length() > 255) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.length"));
        }
        if (applicationInsightsName.endsWith(".")) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.point"));
        }
        if (applicationInsightsName.endsWith(" ") || applicationInsightsName.startsWith(" ")) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.space"));
        }
        final Pattern pattern = Pattern.compile(APP_INSIGHTS_NAME_INVALID_CHARACTERS);
        final Matcher matcher = pattern.matcher(applicationInsightsName);
        final Set<String> invalidCharacters = new HashSet<>();
        while (matcher.find()) {
            invalidCharacters.add(matcher.group());
        }
        if (!invalidCharacters.isEmpty()) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.invalidChar", String.join(",", invalidCharacters)));
        }
    }

    public static void validateSpringCloudAppName(final String name, final SpringCloudCluster cluster) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException(message("springCloud.app.name.validate.empty"));
        } else if (!name.matches(SPRING_CLOUD_APP_NAME_PATTERN)) {
            throw new IllegalArgumentException(message("springCloud.app.name.validate.invalid"));
        } else {
            if (cluster.app(name).exists()) {
                throw new IllegalArgumentException(message("springCloud.app.name.validate.exist", name));
            }
        }
    }


    private static void cacheAndThrow(Map exceptionCache, Object key, String errorMessage) {
        exceptionCache.put(key, errorMessage);
        throw new IllegalArgumentException(errorMessage);
    }

    private static void throwCachedValidationResult(String errorMessage) {
        if (StringUtils.isNotEmpty(errorMessage)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
