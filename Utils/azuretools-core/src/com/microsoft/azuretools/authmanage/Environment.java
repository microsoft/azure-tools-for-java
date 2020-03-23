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

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.AzureEnvironment;
import org.apache.commons.lang3.StringUtils;

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
