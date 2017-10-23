package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.AzureEnvironment;

public enum Environment {
    GLOBAL("GLOBAL") {
        @Override
        public AzureEnvironment getAzureEnvironment() {
            return AzureEnvironment.AZURE;
        }
    },
    CHINA("CHINA") {
        @Override
        public AzureEnvironment getAzureEnvironment() {
            return AzureEnvironment.AZURE_CHINA;
        }
    },
    GERMAN("GERMAN") {
        @Override
        public AzureEnvironment getAzureEnvironment() {
            return AzureEnvironment.AZURE_GERMANY;
        }
    },
    US_GOVERNMENT("US_GOVERNMENT") {
        @Override
        public AzureEnvironment getAzureEnvironment() {
            return AzureEnvironment.AZURE_US_GOVERNMENT;
        }
    };

    private final String envName;

    Environment(String name) {
        this.envName = name;
    }

    public abstract AzureEnvironment getAzureEnvironment();

    public String getName() {
        return this.envName;
    }
}
