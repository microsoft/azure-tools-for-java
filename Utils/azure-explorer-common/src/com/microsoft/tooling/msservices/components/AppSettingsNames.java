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