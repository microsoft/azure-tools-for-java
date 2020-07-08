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

package com.microsoft.azuretools.sdkmanage;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.management.AzureEnvironment;
import com.azure.resourcemanager.Azure;
import com.azure.resourcemanager.resources.fluentcore.profile.AzureProfile;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class IdentityUtils {

    public static boolean validateIdentityCredential(TokenCredential tokenCredential, String azureEnvironment) {
        try {
            Azure.configure()
                    .withHttpClient(new NettyAsyncHttpClientBuilder().build())
                    .authenticate(tokenCredential, new AzureProfile(parseAzureEnvironment(azureEnvironment)))
                    .subscriptions()
                    .list();
            return true;
        } catch (Exception e) {
            // swallow exception, as we could only validate identity credential by exception
            return false;
        }
    }

    // Copied from AzureAuthHelper, create this new function for parse track 2 AzureEnvironment
    // https://github.com/microsoft/azure-maven-plugins/blob/develop/azure-auth-helper/src/main/java/com/microsoft/azure/auth/AzureAuthHelper.java#L101
    public static AzureEnvironment parseAzureEnvironment(String environment) {
        if (StringUtils.isEmpty(environment)) {
            return AzureEnvironment.AZURE;
        }

        switch (environment.toUpperCase(Locale.ENGLISH)) {
            case "AZURE_CHINA":
            case "AZURECHINACLOUD": // this value comes from azure cli
                return AzureEnvironment.AZURE_CHINA;
            case "AZURE_GERMANY":
            case "AZUREGERMANCLOUD": // the TYPO comes from azure cli: https://docs.microsoft.com/en-us/azure/germany/germany-get-started-connect-with-cli
                return AzureEnvironment.AZURE_GERMANY;
            case "AZURE_US_GOVERNMENT":
            case "AZUREUSGOVERNMENT": // this value comes from azure cli
                return AzureEnvironment.AZURE_US_GOVERNMENT;
            default:
                return AzureEnvironment.AZURE;
        }
    }

    private IdentityUtils() {

    }
}
