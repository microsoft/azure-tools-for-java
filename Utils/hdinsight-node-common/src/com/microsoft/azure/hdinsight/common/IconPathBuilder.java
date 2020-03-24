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

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class IconPathBuilder {
    private static String picSuffix = ".png";
    private static String darkTheme = "dark";
    private static String lightTheme = "light";
    private static String bigSize = "16x";
    private static String smallSize = "13x";
    private static String defaultPrefix = "/icons/";

    private boolean isDarkTheme;
    private boolean isBigSize;
    private String pathPrefix;
    private String picName;

    private IconPathBuilder(@NotNull String picName) {
        this.isDarkTheme = DefaultLoader.getUIHelper().isDarkTheme();
        this.isBigSize = false;
        this.pathPrefix = defaultPrefix;
        this.picName = picName;
    }

    public static IconPathBuilder custom(@NotNull String picName) {
        return new IconPathBuilder(picName);
    }

    public IconPathBuilder setPathPrefix(@NotNull String pathPrefix) {
        this.pathPrefix = pathPrefix;
        return this;
    }

    public IconPathBuilder setBigSize() {
        this.isBigSize = true;
        return this;
    }

    public String build() {
        return this.pathPrefix.concat(String.join("_"
                        , this.picName
                        , this.isDarkTheme ? darkTheme : lightTheme
                        , this.isBigSize ? bigSize : smallSize
                ).concat(picSuffix));
    }
}
