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

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.SharedKeyCredential;
import com.microsoft.azure.storage.core.Utility;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import rx.Observable;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class SharedKeyHttpObservable extends HttpObservable {
    public static String ApiVersion = "2018-11-09";
    private SharedKeyCredential cred;
    private HeaderGroup defaultHeaders;

    public SharedKeyHttpObservable(String accountName, String accessKey) {
        defaultHeaders = new HeaderGroup();
        defaultHeaders.addHeader(new BasicHeader("x-ms-client-request-id", UUID.randomUUID().toString()));
        defaultHeaders.addHeader(new BasicHeader("x-ms-date", Utility.getGMTTime()));
        defaultHeaders.addHeader(new BasicHeader("x-ms-version", ApiVersion));
        defaultHeaders.addHeader(new BasicHeader("Authorization", ""));
        defaultHeaders.addHeader(new BasicHeader("Content-Type", "application/json"));

        setDefaultHeaderGroup(defaultHeaders);
        try {
            this.cred = new SharedKeyCredential(accountName, accessKey);
        } catch (IllegalArgumentException ex) {
            log().warn("Create shared key credential encounter exception", ex);
            throw new IllegalArgumentException("Can't create shared key credential.Please check access key");
        }
    }

    public SharedKeyHttpObservable setContentType(@NotNull String type) {
        getDefaultHeaderGroup().updateHeader(new BasicHeader("Content-Type", type));
        return this;
    }

    @Override
    public Observable<CloseableHttpResponse> request(final HttpRequestBase httpRequest,
                                                     @Nullable final HttpEntity entity,
                                                     @Nullable final List<NameValuePair> parameters,
                                                     @Nullable final List<Header> addOrReplaceHeaders) {
        // We add necessary information to a temporary header group which is used to generate shared keys
        final HeaderGroup headerGroup = new HeaderGroup();
        headerGroup.setHeaders(getDefaultHeaderGroup().getAllHeaders());
        if (entity != null) {
            // We need to set content-length to generate shared key. What need to be point out is that the
            // HttpObservable auto adds this header and calculates length when executing, so the content-length header
            // cannot be added to default header group in case of duplication.
            headerGroup.addHeader(new BasicHeader("Content-Length", String.valueOf(entity.getContentLength())));
        }
        if (addOrReplaceHeaders != null) {
            addOrReplaceHeaders.stream().forEach(header -> headerGroup.addHeader(header));
        }
        String key = cred.generateSharedKey(httpRequest, headerGroup, parameters);

        getDefaultHeaderGroup().updateHeader(new BasicHeader("Authorization", key));

        return super.request(httpRequest, entity, parameters, addOrReplaceHeaders);
    }

    @Override
    @Nullable
    public Header[] getDefaultHeaders() throws IOException {
        return defaultHeaders.getAllHeaders();
    }
}
