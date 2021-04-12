/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.arcadia.sdk.common.livy.interactive;

import com.microsoft.azure.arcadia.sdk.common.MfaEspSparkHttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.SparkSession;

import java.net.URI;

public class MfaEspSparkSession extends SparkSession {
    private final MfaEspSparkHttpObservable http;

    public MfaEspSparkSession(final String name, final URI baseUrl, final String tenantId) {
        super(name, baseUrl);
        this.http = new MfaEspSparkHttpObservable(tenantId);
    }

    @Override
    public MfaEspSparkHttpObservable getHttp() {
        return http;
    }
}
