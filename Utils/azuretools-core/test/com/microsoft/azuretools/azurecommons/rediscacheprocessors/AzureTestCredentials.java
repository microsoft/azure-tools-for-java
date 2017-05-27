package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;

import java.io.IOException;

public class AzureTestCredentials extends ApplicationTokenCredentials {
    public AzureTestCredentials() {
        super("", "", "", AzureEnvironment.AZURE);
    }

    @Override
    public String getToken(String resource) throws IOException {
        return "https:/asdd.com";
    }
}
