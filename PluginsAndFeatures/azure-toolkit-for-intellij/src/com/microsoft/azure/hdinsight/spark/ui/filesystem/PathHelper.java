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
package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import java.net.URI;

public class PathHelper {

    // get http://hostname.suffix from  http://hostname.suffix/a/b/c
    public static String getHost(URI uri) {
        return URI.create(String.join("://", uri.getScheme(), uri.getHost())).toString();
    }

    public static String getFullPath(URI uri, String path) {
        return String.join("/", uri.toString(), path);
    }

    // get a/b/c from http://hostname.suffix/a/b/c
    public static String getPath(URI uri) {
        return uri.getPath().length() == 0 ? "/" : uri.getPath().substring(1);
    }

    // get http://hostname.suffix/a/b from http://hostname.suffix/a/b/c
    public static String getParentPath(URI uri) {
        String parent = uri.resolve(".").toString();
        return parent.lastIndexOf("/") == parent.length() - 1 ? parent.substring(0, parent.length() - 1) : parent;
    }

    public static String getRelativePath(URI parent, URI child) {
        String relative = parent.relativize(child).toString();
        return relative.length() == 0 ? "/" : relative;
    }
}
