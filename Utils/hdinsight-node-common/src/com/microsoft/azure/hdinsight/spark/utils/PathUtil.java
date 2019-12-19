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

package com.microsoft.azure.hdinsight.spark.utils;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

// Codes in this file is referred from JetBrains/intellij-community. Check the link below for details.
// https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/util/PathUtil.java
public class PathUtil {
    public static String getParentPath(@NotNull String path) {
        if (path.length() == 0) return "";
        int end = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (end == path.length() - 1) {
            end = getLastIndexOfPathSeparator(path, end);
        }
        return end == -1 ? "" : path.substring(0, end);
    }

    private static int getLastIndexOfPathSeparator(@NotNull String path, int end) {
        return Math.max(path.lastIndexOf('/', end - 1), path.lastIndexOf('\\', end - 1));
    }

}
