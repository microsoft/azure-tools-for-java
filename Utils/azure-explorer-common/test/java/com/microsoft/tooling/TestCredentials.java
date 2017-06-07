package com.microsoft.tooling;

import java.io.IOException;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;

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
