/*
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
 *
 */

package com.microsoft.azure.hdinsight.common;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UnknownFormatConversionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WasbUri {
    public static final Pattern WASB_URI_PATTERN = Pattern.compile(
            "^wasb[s]?://(?<container>[^/.]+)@(?<storageAccount>[^/.]+)\\.blob\\.(?<endpointSuffix>[^/]+)"
                    + "(:(?<port>[0-9]+))?(/(?<path>.*))?$",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern HTTP_URI_PATTERN = Pattern.compile(
            "^http[s]?://(?<storageAccount>[^/.]+)\\.blob\\.(?<endpointSuffix>[^/]+)/(?<container>[^/.]+)"
                    + "(:(?<port>[0-9]+))?(/(?<path>.*))?$",
            Pattern.CASE_INSENSITIVE);

    private final URI rawUri;
    private final LaterInit<String> container = new LaterInit<>();
    private final LaterInit<String> storageAccount = new LaterInit<>();
    private final LaterInit<String> endpointSuffix = new LaterInit<>();
    private final LaterInit<String> absolutePath = new LaterInit<>();

    private WasbUri(final URI rawUri) {
        this.rawUri = rawUri;
    }

    public URI getRawUri() {
        return rawUri;
    }

    public URI getUri() {
        return URI.create(String.format("wasbs://%s@%s.blob.%s%s",
                getContainer(), getStorageAccount(), getEndpointSuffix(), getAbsolutePath()));
    }

    public URL getUrl() {
        try {
            return URI.create(String.format("https://%s.blob.%s/%s%s",
                    getStorageAccount(), getContainer(), getEndpointSuffix(), getAbsolutePath())).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public String getContainer() {
        return this.container.get();
    }

    public String getStorageAccount() {
        return this.storageAccount.get();
    }

    public String getEndpointSuffix() {
        return this.endpointSuffix.get();
    }

    public String getAbsolutePath() {
        return this.absolutePath.get();
    }

    public String getHadoopBlobFsPropertyKey() {
        return String.format("fs.azure.account.key.%s.blob.%s", getStorageAccount(), getEndpointSuffix());
    }

    @Override
    public String toString() {
        return rawUri.toString();
    }

    public static WasbUri parse(final String blobUri) {
        Matcher matcher;
        if (StringUtils.startsWithIgnoreCase(blobUri, "wasb")) {
            matcher = WASB_URI_PATTERN.matcher(blobUri);
        } else if (StringUtils.startsWithIgnoreCase(blobUri, "http")) {
            matcher = HTTP_URI_PATTERN.matcher(blobUri);
        } else {
            throw new UnknownFormatConversionException("Unsupported Azure blob URI Scheme: " + blobUri);
        }

        if (matcher.matches()) {
            final WasbUri wasbUri = new WasbUri(URI.create(blobUri));

            wasbUri.container.set(matcher.group("container"));
            wasbUri.storageAccount.set(matcher.group("storageAccount"));
            wasbUri.endpointSuffix.set(matcher.group("endpointSuffix"));

            final String pathMatched = matcher.group("path");
            final String absolutePathMatched = URI.create("/" + (pathMatched == null ? "" : pathMatched)).getPath();
            wasbUri.absolutePath.set(absolutePathMatched);

            return wasbUri;
        }

        throw new UnknownFormatConversionException("Unmatched Azure blob URI: " + blobUri);
    }

    public static boolean isType(final String uri) {
        return WASB_URI_PATTERN.matcher(uri).matches() || HTTP_URI_PATTERN.matcher(uri).matches();
    }
}
