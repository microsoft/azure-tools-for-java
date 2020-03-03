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

public class AdlUri extends AzureStorageUri {
    public static final Pattern ADL_URI_PATTERN = Pattern.compile(
            "^adl[s]?://(?<storageName>[^/.]+)\\.azuredatalakestore\\.net"
                    + "(:(?<port>[0-9]+))?(/(?<path>.*))?$",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern HTTP_URI_PATTERN = Pattern.compile(
            "^http[s]?://(?<storageName>[^/.]+)\\.azuredatalakestore\\.net"
                    + "(:(?<port>[0-9]+))?/webhdfs/v1(/(?<path>.*))?$",
            Pattern.CASE_INSENSITIVE);

    private final LaterInit<String> storageName = new LaterInit<>();

    private AdlUri(URI rawUri) {
        super(rawUri);
    }

    @Override
    public URI getUri() {
        return URI.create(String.format("adl://%s.azuredatalakestore.net%s",
                getStorageName(), getPath()));
    }

    @Override
    public URL getUrl() {
        try {
            return URI.create(String.format("https://%s.azuredatalakestore.net/webhdfs/v1%s",
                    getStorageName(), getPath())).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public String getStorageName() {
        return this.storageName.get();
    }

    @Override
    AzureStorageUri parseUri(URI encodedUri) {
        return AdlUri.parse(encodedUri.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStorageName(), getPath());
    }

    public static boolean isType(@Nullable final String uri) {
        return uri != null && (ADL_URI_PATTERN.matcher(uri).matches() || HTTP_URI_PATTERN.matcher(uri).matches());
    }

    public static AdlUri parse(final String adlUri) {
        Matcher matcher;
        if (StringUtils.startsWithIgnoreCase(adlUri, "adl")) {
            matcher = ADL_URI_PATTERN.matcher(adlUri);
        } else if (StringUtils.startsWithIgnoreCase(adlUri, "http")) {
            matcher = HTTP_URI_PATTERN.matcher(adlUri);
        } else {
            throw new UnknownFormatConversionException("Unsupported Azure ADLS Gen 1 URI Scheme: " + adlUri);
        }

        if (!matcher.matches()) {
            throw new UnknownFormatConversionException("Unmatched Azure ADLS Gen 1 URI: " + adlUri);
        }

        final AdlUri uri = new AdlUri(URI.create(adlUri));

        uri.storageName.set(matcher.group("storageName"));

        final String pathMatched = matcher.group("path");
        final String relativePathMatched = URI.create("/" + (pathMatched == null ? "" : pathMatched)).getPath();
        uri.path.set(URI.create(relativePathMatched));

        // TODO: matcher.group("port") is ignored now, should be fixed later

        return uri;
    }
}
