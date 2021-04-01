/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.storage.webhdfs;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class WebHdfsParamsBuilder {
    public List<NameValuePair> params = new ArrayList<>();

    public WebHdfsParamsBuilder() {
    }

    public WebHdfsParamsBuilder(@NotNull String operation) {
        params.add(new BasicNameValuePair("op", operation));
    }

    public WebHdfsParamsBuilder setOverwrite(@NotNull String value) {
        params.add(new BasicNameValuePair("overwrite", value));
        return this;
    }

    public WebHdfsParamsBuilder setPermission(@NotNull String value) {
        params.add(new BasicNameValuePair("permission", value));
        return this;
    }

    public List<NameValuePair> build() {
        return params;
    }
}
