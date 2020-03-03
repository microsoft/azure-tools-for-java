/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
