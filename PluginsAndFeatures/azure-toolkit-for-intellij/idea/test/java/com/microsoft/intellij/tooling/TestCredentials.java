/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.tooling;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;

import java.io.IOException;

public class TestCredentials extends ApplicationTokenCredentials {
    public TestCredentials() {
        super("", "", "", AzureEnvironment.AZURE);
    }

    @Override
    public String getToken(String resource) throws IOException {
        if (!IntegrationTestBase.IS_MOCKED) {
            // TODO: non-mock case;
        }
        return "https:/asdd.com";
    }
}
