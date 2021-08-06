/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.fluent.models.ResourceNameAvailabilityInner;
import com.azure.resourcemanager.appservice.models.CheckNameResourceTypes;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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
        final AzureResourceManager azureResourceManager = Azure.az(AzureAppService.class).getAzureResourceManager(subscriptionId);
        final ResourceNameAvailabilityInner result = azureResourceManager.webApps().manager().serviceClient()
                .getResourceProviders().checkNameAvailability(appServiceName, CheckNameResourceTypes.MICROSOFT_WEB_SITES);
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
            final ResourceGroup rg = Azure.az(AzureGroup.class).get(subscriptionId, resourceGroup);
            if (rg != null) {
                cacheAndThrow(resourceGroupValidationCache, subscriptionId, message("appService.resourceGroup.validate.exist"));
            }
        } catch (ManagementException e) {
            // swallow exception for get resources
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
        } else if (Objects.nonNull(cluster) && cluster.app(name).exists()) {
            throw new IllegalArgumentException(message("springCloud.app.name.validate.exist", name));
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
