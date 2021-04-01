/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.storage.adlsgen2;

import com.microsoft.azure.hdinsight.sdk.storage.webhdfs.WebHdfsParamsBuilder;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.Collections;

public class ADLSGen2ParamsBuilder extends WebHdfsParamsBuilder {
    public ADLSGen2ParamsBuilder() {
    }

    public ADLSGen2ParamsBuilder setAction(@NotNull String value) {
        params.add(new BasicNameValuePair("action", value));
        return this;
    }

    public ADLSGen2ParamsBuilder setPosition(@NotNull long value) {
        params.add(new BasicNameValuePair("position", String.valueOf(value)));
        return this;
    }

    public ADLSGen2ParamsBuilder setResource(@NotNull String value) {
        params.add(new BasicNameValuePair("resource", value));
        return this;
    }

    public ADLSGen2ParamsBuilder setDirectory(@NotNull String value) {
        params.add(new BasicNameValuePair("directory", value));
        return this;
    }

    public ADLSGen2ParamsBuilder enableRecursive(@NotNull boolean value) {
        params.add(new BasicNameValuePair("recursive", String.valueOf(value)));
        return this;
    }
}
