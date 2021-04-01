/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

import com.microsoft.azure.management.Azure;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class AzureRegisterProviderNamespaces {
    private static final String[] namespaces = new String[]{"Microsoft.Resources", "Microsoft.Network", "Microsoft.Compute", "Microsoft.KeyVault",
        "Microsoft.Storage", "Microsoft.Web", "Microsoft.Authorization", "Microsoft.HDInsight", "Microsoft.DBforMySQL"};

    public static void registerAzureNamespaces(Azure azureInstance) {

        try {
            Arrays.stream(namespaces).parallel()
                    .map(azureInstance.providers()::getByName)
                    .filter(provider -> !StringUtils.equalsIgnoreCase("Registered", provider.registrationState()))
                    .forEach(provider -> azureInstance.providers().register(provider.namespace()));
        } catch (Exception ignored) {
            // No need to handle this for now since this functionality will be eventually removed once the Azure SDK
            //  something similar
        }
    }
}
