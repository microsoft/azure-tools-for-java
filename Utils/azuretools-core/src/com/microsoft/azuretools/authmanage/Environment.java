/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.AzureEnvironment;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Stream;

public abstract class Environment {
    public static final Environment GLOBAL = new Environment("GLOBAL") {
        @Override
        public AzureEnvironment getAzureEnvironment() {
            return AzureEnvironment.AZURE;
        }
    };

    public static final Environment CHINA = new Environment("CHINA") {
        @Override
        public AzureEnvironment getAzureEnvironment() {
            return AzureEnvironment.AZURE_CHINA;
        }
    };

    public static final Environment GERMAN = new Environment("GERMAN") {
        @Override
        public AzureEnvironment getAzureEnvironment() {
            return AzureEnvironment.AZURE_GERMANY;
        }
    };

    public static final Environment US_GOVERNMENT = new Environment("US_GOVERNMENT") {
        @Override
        public AzureEnvironment getAzureEnvironment() {
            return AzureEnvironment.AZURE_US_GOVERNMENT;
        }
    };

    public static final List<Environment> ENVIRONMENT_LIST = ImmutableList.of(GLOBAL, CHINA, GERMAN, US_GOVERNMENT);

    private final String envName;

    Environment(String name) {
        this.envName = name;
    }

    public static Environment valueOf(String name) throws IllegalAccessException {
        return Stream.of(GLOBAL, CHINA, GERMAN, US_GOVERNMENT)
                .filter(env -> StringUtils.equalsIgnoreCase(env.envName, name))
                .findFirst()
                .orElseThrow(() -> new IllegalAccessException("No such Environment defined for " + name));
    }

    public abstract AzureEnvironment getAzureEnvironment();

    public String getName() {
        return this.envName;
    }
}
