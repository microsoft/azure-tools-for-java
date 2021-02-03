/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
