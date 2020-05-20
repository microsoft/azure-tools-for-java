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

package com.microsoft.azure.hdinsight.sdk.storage.adlsgen2;

import com.microsoft.azure.storage.Constants;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.HeaderGroup;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class SharedKeyCredential {

    private final String accountName;
    private final byte[] accountKey;

    public SharedKeyCredential(String accountName, String accountKey) {
        this.accountName = accountName;
        this.accountKey = Base64.getDecoder().decode(accountKey);
    }

    private String buildStringToSign(HttpRequestBase request, HeaderGroup httpHeaders, List<NameValuePair> pairs) {
        String contentLength = getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_LENGTH);
        contentLength = contentLength.equals("0") ? Constants.EMPTY_STRING : contentLength;

        return String.join("\n",
                request.getMethod().toString(),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_ENCODING),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_LANGUAGE),
                contentLength,
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_MD5),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.CONTENT_TYPE),
                // x-ms-date header exists, so don't sign date header
                Constants.EMPTY_STRING,
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.IF_MODIFIED_SINCE),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.IF_MATCH),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.IF_NONE_MATCH),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.IF_UNMODIFIED_SINCE),
                getStandardHeaderValue(httpHeaders, Constants.HeaderConstants.RANGE),
                getCanonicalizedHeader(httpHeaders),
                getCanonicalizedResource(request.getURI(), pairs));
    }

    private String getCanonicalizedHeader(HeaderGroup headerGroup) {
        List<Header> headers = Arrays.asList(headerGroup.getAllHeaders());
        Collections.sort(headers, Comparator.comparing(Header::getName));
        StringBuffer buffer = new StringBuffer();
        for (Header header : headers) {
            if (header.getName().startsWith("x-ms-")) {
                buffer.append(String.format("%s:%s", header.getName(), header.getValue()));
                buffer.append("\n");
            }
        }

        return buffer.toString().substring(0, buffer.length() - 1);
    }

    private void appendCanonicalizedElement(final StringBuilder builder, final String element) {
        builder.append("\n");
        builder.append(element);
    }

    private String getCanonicalizedResource(URI requestURL, List<NameValuePair> pairs) {
        // Resource path
        final StringBuilder canonicalizedResource = new StringBuilder("/");
        canonicalizedResource.append(this.accountName);

        // Note that AbsolutePath starts with a '/'.
        if (requestURL.getPath().length() > 0) {
            canonicalizedResource.append(requestURL.getPath());
        } else {
            canonicalizedResource.append('/');
        }

        // check for no query params and return
        if (pairs.size() == 0) {
            return canonicalizedResource.toString();
        }

        Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
        for (NameValuePair pair : pairs) {
            String key = pair.getName();
            if (!queryParams.containsKey(key)) {
                queryParams.put(key, new ArrayList<>());
            }

            queryParams.get(key).add(pair.getValue());
        }


        ArrayList<String> queryParamNames = new ArrayList<>(queryParams.keySet());
        Collections.sort(queryParamNames);

        for (String queryParamName : queryParamNames) {
            final List<String> queryParamValues = queryParams.get(queryParamName);
            Collections.sort(queryParamValues);
            String queryParamValuesStr = String.join(",", queryParamValues.toArray(new String[]{}));
            canonicalizedResource.append("\n").append(queryParamName.toLowerCase(Locale.ROOT)).append(":")
                    .append(queryParamValuesStr);
        }

        // append to main string builder the join of completed params with new line
        return canonicalizedResource.toString();
    }


    private String getStandardHeaderValue(HeaderGroup headers, final String headerName) {
        Header header = headers.getFirstHeader(headerName);
        return header == null ? Constants.EMPTY_STRING
                : header.getName().startsWith(Constants.PREFIX_FOR_STORAGE_HEADER)
                ? String.format("%s:%s", header.getName(), header.getValue()) : header.getValue();
    }

    private String computeHmac256(String stringToSign) throws InvalidKeyException {
        try {
            /*
            We must get a new instance of the Mac calculator for each signature calculated because the instances are
            not threadsafe and there is some suggestion online that they may not even be safe for reuse, so we use a
            new one each time to be sure.
             */
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            hmacSha256.init(new SecretKeySpec(this.accountKey, "HmacSHA256"));
            byte[] utf8Bytes = stringToSign.getBytes(Constants.UTF8_CHARSET);
            return Base64.getEncoder().encodeToString(hmacSha256.doFinal(utf8Bytes));
        } catch (final UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    public String generateSharedKey(HttpRequestBase request, HeaderGroup httpHeaders, List<NameValuePair> pairs) {
        try {
            String stringToSign = buildStringToSign(request, httpHeaders, pairs);
            String computedBase64Signature = computeHmac256(stringToSign);
            return String.format("%s %s:%s", "SharedKey", this.accountName, computedBase64Signature);
        } catch (InvalidKeyException e) {
            return null;
        }
    }
}

