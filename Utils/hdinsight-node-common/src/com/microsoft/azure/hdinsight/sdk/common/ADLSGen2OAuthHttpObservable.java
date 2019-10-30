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

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.List;

public class ADLSGen2OAuthHttpObservable extends SharedKeyHttpObservable {
    private static final String resource = "https://storage.azure.com/";
    private String tenantId;

    public ADLSGen2OAuthHttpObservable(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public ADLSGen2OAuthHttpObservable setAuthorization(HttpRequestBase req, List<NameValuePair> pairs) {
        try {
            getDefaultHeaderGroup().updateHeader(new BasicHeader("Authorization", "Bearer "+AdAuthManager.getInstance().getAccessToken(tenantId, resource, PromptBehavior.Auto)));
        } catch (IOException e) {
        }

        return this;
    }
}
