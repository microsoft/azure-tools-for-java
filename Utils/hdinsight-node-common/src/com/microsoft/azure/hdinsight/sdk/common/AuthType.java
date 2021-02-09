/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

public enum AuthType {
    BasicAuth("Basic Authentication"),
    AADAuth("Azure Account"),
    NoAuth("No Authentication"),
    NotSupported("Not supported");

    private String type;

    AuthType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }
}
