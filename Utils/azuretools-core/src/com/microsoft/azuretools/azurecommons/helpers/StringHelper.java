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

package com.microsoft.azuretools.azurecommons.helpers;

import java.util.List;
import java.util.regex.Pattern;

public class StringHelper {
    // pattern for cluster URL. e.g. https://sparkcluster.azurehdinsight.net
    private static final Pattern CLUSTER_URL_PATTERN = Pattern.compile("https://([^/.]+\\.)+[^/.]+/?$");

    public static boolean isNullOrWhiteSpace(String str) {
        if (str == null) {
            return true;
        }

        int len = str.length();
        for (int i = 0; i < len; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static String concat(@NotNull String... args) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < args.length; ++i) {
            stringBuilder.append(args[i]);
        }

        return stringBuilder.toString();
    }

    public static String join(@NotNull String delimiter, @NotNull List<String> args) {
        StringBuffer stringBuffer=new StringBuffer();

        for (int i=0; i < args.size(); ++i) {
            stringBuffer.append(args.get(i));
            if (i != args.size() - 1) {
                stringBuffer.append(delimiter);
            }
        }

        return stringBuffer.toString();
    }

    public static String getClusterNameFromEndPoint(@NotNull String endpoint) {
        if (CLUSTER_URL_PATTERN.matcher(endpoint).find()) {
            return endpoint.split("\\.")[0].substring(8);
        }

        return null;
    }
}
