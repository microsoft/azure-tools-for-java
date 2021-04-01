/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.mock;

import org.apache.hadoop.fs.*;

import java.net.URI;
import java.net.URISyntaxException;


class MockDfs extends LocalFileSystem {
    public MockDfs() {
        super(new MockRawLocalFileSystem());
    }

    @Override
    public URI getUri() {
        return URI.create("mockDfs:///");
    }

    @Override
    public String getScheme() {
        return "mockDfs";
    }
}
