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

public class AbfsUri {
    public static final String AdlsGen2PathPattern = "^(?<schema>abfss?)://(?<fileSystem>[^/.\\s]+)@(?<accountName>[^/.\\s]+)(\\.)(dfs\\.core\\.windows\\.net)(?<relativePath>(/[^\\s]+)*/?)$";
    public static final String AdlsGen2RestfulPathPattern = "^(?<schema>https?)://(?<accountName>[^/.\\s]+)(\\.)(dfs\\.core\\.windows\\.net)(/)(?<fileSystem>[^/.\\s]+)(?<relativePath>(/[^\\s]+)*/?)$";
    public static final Pattern ABFS_URI_PATTERN = Pattern.compile(AdlsGen2PathPattern, Pattern.CASE_INSENSITIVE);
    public static final Pattern HTTP_URI_PATTERN = Pattern.compile(AdlsGen2RestfulPathPattern, Pattern.CASE_INSENSITIVE);

    private final URI rawUri;
    private final LaterInit<String> fileSystem = new LaterInit<>();
    private final LaterInit<String> accountName = new LaterInit<>();
    private final LaterInit<String> relativePath = new LaterInit<>();

    private AbfsUri(URI rawUri) {
        this.rawUri = rawUri;
    }

    public URI getRawUri() {
        return rawUri;
    }

    public String getFileSystem() {
        return fileSystem.get();
    }

    public String getAccountName() {
        return accountName.get();
    }

    public String getRelativePath() {
        return relativePath.get();
    }

    public AbfsUri getRoot() {
        return AbfsUri.parse(String.format("abfs://%s@%s.dfs.core.windows.net", getFileSystem(), getAccountName()));
    }

    public URI getUri() {
        return URI.create(String.format("abfs://%s@%s.dfs.core.windows.net%s",
                getFileSystem(), getAccountName(), getRelativePath()));
    }

    public URL getUrl() {
        try {
            return URI.create(String.format("https://%s.dfs.core.windows.net/%s%s",
                    getAccountName(), getFileSystem(), getRelativePath())).toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    // get subPath starting without "/" except when subPath is empty
    public URI getDirectoryParam() {
        return getRelativePath().length() == 0 || getRelativePath().equals("/")
                ? URI.create("/")
                : URI.create(getRelativePath().substring(1));
    }

    public static AbfsUri parse(final String rawUri) {
        Matcher matcher;
        if (StringUtils.startsWithIgnoreCase(rawUri, "abfs")) {
            matcher = ABFS_URI_PATTERN.matcher(rawUri);
        } else if (StringUtils.startsWithIgnoreCase(rawUri, "http")) {
            matcher = HTTP_URI_PATTERN.matcher(rawUri);
        } else {
            throw new UnknownFormatConversionException("Unsupported ADLS Gen2 URI Scheme: " + rawUri);
        }

        if (matcher.matches()) {
            AbfsUri abfsUri = new AbfsUri(URI.create(rawUri));
            abfsUri.accountName.set(matcher.group("accountName"));
            abfsUri.fileSystem.set(matcher.group("fileSystem"));
            abfsUri.relativePath.set(matcher.group("relativePath"));
            return abfsUri;
        }

        throw new UnknownFormatConversionException("Unmatched ADLS Gen2 URI: " + rawUri);
    }

    public static boolean isType(@Nullable final String uri) {
        return uri != null && (ABFS_URI_PATTERN.matcher(uri).matches() || HTTP_URI_PATTERN.matcher(uri).matches());
    }

    @Override
    public String toString() {
        return rawUri.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbfsUri abfsUri = (AbfsUri) o;
        return getFileSystem().equals(abfsUri.getFileSystem()) &&
                getAccountName().equals(abfsUri.getAccountName()) &&
                getRelativePath().equals(abfsUri.getRelativePath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileSystem(), getAccountName(), getRelativePath());
    }
}
