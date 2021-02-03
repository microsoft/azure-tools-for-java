/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azure.hdinsight.common.AdlUri;
import com.microsoft.azure.hdinsight.common.AzureStorageUri;
import com.microsoft.azure.hdinsight.common.WasbUri;

import java.net.URI;
import java.util.UnknownFormatConversionException;

public enum StorageAccountType {
    BLOB(WasbUri::parse),
    ADLS(AdlUri::parse),
    ADLSGen2(AbfsUri::parse),
    UNKNOWN(uri -> {
        throw new UnknownFormatConversionException("Unknown Storage Account Type URI: " + uri);
    });

    public interface Parser {
        AzureStorageUri parse(String uri);
    }

    StorageAccountType(Parser parser) {
        this.parser = parser;
    }

    private final Parser parser;

    public static StorageAccountType parseUri(String uri) {
        try {
            return parseUri(URI.create(uri));
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }

    public static StorageAccountType parseUri(URI uri) {
        if (WasbUri.isType(uri.toString())) {
            return BLOB;
        } else if (AdlUri.isType(uri.toString())) {
            return ADLS;
        } else if (AbfsUri.isType(uri.toString())) {
            return ADLSGen2;
        } else {
            return UNKNOWN;
        }
    }

    public AzureStorageUri parse(String uri) {
        return parser.parse(uri);
    }
}
