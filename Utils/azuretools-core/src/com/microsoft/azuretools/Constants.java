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

package com.microsoft.azuretools;

public class Constants {
    public static String resourceVault = "https://vault.azure.net";

    public static String tenant = "72f988bf-86f1-41af-91ab-2d7cd011db47";
    public static String clientId = "61d65f5a-6e3b-468b-af73-a033f5098c5c";
    public static String redirectUri = "https://msopentech.com/";
    public static int connection_read_timeout_ms = 10000;

    public static final String FILE_NAME_AAD_PROVIDER = "AadProvider.json";
    public static final String FILE_NAME_AUTH_METHOD_DETAILS = "AuthMethodDetails.json";
    public static final String FILE_NAME_CORE_LIB_LOG = "corelibs.log";
    public static final String FILE_NAME_SUBSCRIPTIONS_DETAILS_AT = "subscriptionsDetails-at.json";
    public static final String FILE_NAME_SUBSCRIPTIONS_DETAILS_SP = "subscriptionsDetails-sp.json";
    public static final String FILE_NAME_SUBSCRIPTIONS_DETAILS_AZ = "subscriptionsDetails-az.json";
    public static final String FILE_NAME_SUBSCRIPTIONS_DETAILS_AI_DC = "subscriptionsDetails-ai-dc.json";
    public static final String FILE_NAME_SURVEY_CONFIG = "SurveyConfig.json";
}
