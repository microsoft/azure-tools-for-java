/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.livy.interactive;

import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.SessionKind;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.net.URI;

public class SparkSession extends Session {
    public SparkSession(final String name,
                        final URI baseUrl) {
        this(name, baseUrl, null, null);
    }

    public SparkSession(final String name,
                        final URI baseUrl,
                        final @Nullable String username,
                        final @Nullable String password) {
        super(name, baseUrl, new CreateParameters(SessionKind.SPARK).name(name), username, password);
    }

    @Override
    public SessionKind getKind() {
        return SessionKind.SPARK;
    }
}
