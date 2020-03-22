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
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A class for Azure Storage related URIs
 */
abstract public class AzureStorageUri {
    private final URI rawUri;
    protected final LaterInit<URI> path = new LaterInit<>();

    protected AzureStorageUri(URI rawUri) {
        this.rawUri = rawUri;
    }

    /**
     * Getter of raw URI
     *
     * @return the raw URI
     */
    public URI getRawUri() {
        return rawUri;
    }

    /**
     * Get URI with specified scheme except HTTP and HTTPS
     *
     * @return URI of current instance
     */
    abstract public URI getUri();

    /**
     * Get URL with HTTP/HTTPS only scheme, which is usually used for WEBHDFS accessing
     *
     * @return URL of current instance
     */
    abstract public URL getUrl();

    /**
     * Get decoded URI path like URI.getPath()
     *
     * @return the path of current URI
     */
    public String getPath() {
        return path.get().getPath();
    }

    /**
     * Get Raw URI path like URI.getRawPath()
     *
     * @return raw path of current URI
     */
    public String getRawPath() {
        return path.get().getRawPath();
    }

    /**
     * Resolve target path as URI.resolve(target)
     *
     * @param target the target path to resolve
     * @return resolved Azure storage URI
     */
    public AzureStorageUri resolve(String target) {
        return parseUri(getUri().resolve(target.startsWith("/") ? target : "./" + target));
    }

    /**
     * Parse the String to AzureStorageUri
     * @param encodedUri the encoded Uri to parse
     * @return parsed Azure storage URI
     */
    abstract AzureStorageUri parseUri(URI encodedUri);

    /**
     * Normalize the URI with slash ending
     *
     * @return a new instance with slash ended
     */
    public AzureStorageUri normalizeWithSlashEnding() {
        return parseUri(UriUtil.normalizeWithSlashEnding(getUri()));
    }

    /**
     * Treat current Azure Storage URI as root to resolve the target path
     * eg:
     *   this("https://host/root").resolveAsRoot("/tmp"),
     *   this("https://host/root/").resolveAsRoot("/tmp") and
     *   this("https://host/root/").resolveAsRoot("tmp") will all return "https://host/root/tmp"
     *
     * @param target the target path to resolve
     * @return A new instance with connecting the current URI as root path with target
     */
    public AzureStorageUri resolveAsRoot(String target) {
        return normalizeWithSlashEnding().resolve("./" + target);
    }

    /**
     * Compute relativized path as URI.relativize(target)
     *
     * @param target the target Azure storage URI to relativize
     * @return a new instance with removing both common part path, null for not related
     */
    @Nullable
    public String relativize(AzureStorageUri target) {
        String relativePath = getUri().relativize(target.getUri()).getPath();

        if (relativePath.startsWith("/")) {
            return null;
        }

        return relativePath;
    }

    /**
     * Encode and normalize the relative path in the Azure Storage URI.
     *
     * Note: There is a different behavior between this method and URI.normalize()
     * even if both methods return URI with correct format.
     *
     * URI.create("./:bbb").normalize().toString will return "./:bbb"
     * this.encodeAndNormalizePath("./:bbb") will return ":bbb"
     *
     * @param rawPath un-encoded raw path in the Azure Storage URI
     * @return encoded and normalized path in the Azure Storage URI
     */
    public static String encodeAndNormalizePath(String rawPath) {
        try {
            String encodedPath = URI.create("/").relativize(new URIBuilder().setPath("/" + rawPath).build()).getRawPath();
            return rawPath.startsWith("/") ? "/" + encodedPath : encodedPath;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(
                    String.format("Error when encode raw path %s. Error:%s", rawPath, ex.getMessage()), ex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return hashCode() == o.hashCode();
    }

    @Override
    public String toString() {
        return rawUri.toString();
    }

    @Override
    abstract public int hashCode();
}
