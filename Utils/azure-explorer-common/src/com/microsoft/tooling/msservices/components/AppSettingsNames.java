/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.components;

/**
 * NOTE: If you add new setting names to this list, evaluate whether it should be cleared
 * when the plugin is upgraded/uninstalled and add the setting to the array "settings" in
 * the "cleanTempData" function below. Otherwise your setting will get retained across
 * upgrades which can potentially cause issues.
 */
public class AppSettingsNames {
    public static final String CURRENT_PLUGIN_VERSION = "com.microsoft.intellij.PluginVersion";
    public static final String EXTERNAL_STORAGE_ACCOUNT_LIST = "com.microsoft.intellij.ExternalStorageAccountList";

    public static final String AAD_AUTHENTICATION_RESULTS = "com.microsoft.tooling.msservices.AADAuthenticationResults";
    public static final String O365_USER_INFO = "com.microsoft.tooling.msservices.O365UserInfo";
    public static final String AZURE_SUBSCRIPTIONS = "com.microsoft.intellij.AzureSubscriptions";
    public static final String AZURE_USER_INFO = "com.microsoft.intellij.AzureUserInfo";
    public static final String AZURE_USER_SUBSCRIPTIONS = "com.microsoft.intellij.AzureUserSubscriptions";
    public static final String AAD_TOKEN_CACHE = "com.microsoft.auth.TokenCache";
}
