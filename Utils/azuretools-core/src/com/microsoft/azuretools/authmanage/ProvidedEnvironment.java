/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class ProvidedEnvironment extends Environment {
    @Nullable
    private AzureEnvironment azureEnvironment;

    ProvidedEnvironment(String name) {
        super(name);
    }

    @Override
    @Nullable
    public AzureEnvironment getAzureEnvironment() {
        return azureEnvironment;
    }
}
