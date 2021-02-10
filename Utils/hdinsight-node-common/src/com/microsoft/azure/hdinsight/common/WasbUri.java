/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.UnknownFormatConversionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WasbUri extends AzureStorageUri {
    public static final Pattern WASB_URI_PATTERN = Pattern.compile(
            "^wasb[s]?://(?<container>[^/.]+)@(?<storageAccount>[^/.]+)\\.blob\\.(?<endpointSuffix>[^/]+)"
                    + "(:(?<port>[0-9]+))?(/(?<path>.*))?$",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern HTTP_URI_PATTERN = Pattern.compile(
            "^http[s]?://(?<storageAccount>[^/.]+)\\.blob\\.(?<endpointSuffix>[^/]+)/(?<container>[^/.]+)"
                    + "(:(?<port>[0-9]+))?(/(?<path>.*))?$",
            Pattern.CASE_INSENSITIVE);

    private final LaterInit<String> container = new LaterInit<>();
    private final LaterInit<String> storageAccount = new LaterInit<>();
    private final LaterInit<String> endpointSuffix = new LaterInit<>();

    private WasbUri(final URI rawUri) {
        super(rawUri);
    }

    @Override
    public URI getUri() {
        return URI.create(String.format("wasbs://%s@%s.blob.%s%s",
                getContainer(), getStorageAccount(), getEndpointSuffix(), getPath()));
    }

    @Override
    public URL getUrl() {
        try {
            return URI.create(String.format("https://%s.blob.%s/%s%s",
                    getStorageAccount(), getEndpointSuffix(), getContainer(), getPath())).toURL();
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

    @Override
    AzureStorageUri parseUri(URI encodedUri) {
        return WasbUri.parse(encodedUri.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStorageAccount(), getContainer(), getEndpointSuffix(), getPath());
    }

    public String getHadoopBlobFsPropertyKey() {
        return String.format("fs.azure.account.key.%s.blob.%s", getStorageAccount(), getEndpointSuffix());
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
            final String relativePathMatched = URI.create("/" + (pathMatched == null ? "" : pathMatched)).getPath();
            wasbUri.path.set(URI.create(relativePathMatched));

            return wasbUri;
        }

        throw new UnknownFormatConversionException("Unmatched Azure blob URI: " + blobUri);
    }

    public static boolean isType(@Nullable final String uri) {
        return uri != null && (WASB_URI_PATTERN.matcher(uri).matches() || HTTP_URI_PATTERN.matcher(uri).matches());
    }
}
