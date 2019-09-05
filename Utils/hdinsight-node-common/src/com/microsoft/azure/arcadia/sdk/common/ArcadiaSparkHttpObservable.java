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

package com.microsoft.azure.arcadia.sdk.common;

import com.microsoft.azure.hdinsight.sdk.common.ApiVersionParam;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArcadiaSparkHttpObservable extends AzureHttpObservable {
    public static final String WORKSPACE_HEADER_NAME = "x-ms-workspace-name";
    public static final String ARCADIA_RESOURCE_ID = "5d13f7d7-0567-429c-9880-320e9555e5fc";

    @NotNull
    private String workspaceName;

    public ArcadiaSparkHttpObservable(@NotNull String tenantId, @NotNull String workspaceName) {
        super(tenantId, "");
        this.workspaceName = workspaceName;
    }

    @Override
    public Header[] getDefaultHeaders() throws IOException {
        Header[] defaultHeaders = super.getDefaultHeaders();
        List<Header> headers = Arrays.stream(defaultHeaders)
                .filter(header -> !header.getName().equals(WORKSPACE_HEADER_NAME))
                .collect(Collectors.toList());

        headers.add(new BasicHeader(WORKSPACE_HEADER_NAME, workspaceName));

        return headers.toArray(new Header[0]);
    }

    @Override
    public List<NameValuePair> getDefaultParameters() {
        return super.getDefaultParameters()
                .stream()
                // parameter apiVersion is not needed for arcadia since it's already specified in the path of query url
                .filter(kvPair -> !kvPair.getName().equals(ApiVersionParam.NAME))
                .collect(Collectors.toList());
    }

    @Override
    public String getResourceEndpoint() {
        return ARCADIA_RESOURCE_ID;
    }
}
