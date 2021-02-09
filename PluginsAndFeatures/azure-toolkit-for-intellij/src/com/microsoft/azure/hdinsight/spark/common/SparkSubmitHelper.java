/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azuretools.azurecommons.helpers.StringHelper;

public class SparkSubmitHelper {

    public static boolean isLocalArtifactPath(String path) {
        if (StringHelper.isNullOrWhiteSpace(path)) {
            return false;
        }

        if (path.endsWith("!/")) {
            path = path.substring(0, path.length() - 2);
        }

        return path.endsWith(".jar");
    }

    public static final String HELP_LINK = "http://go.microsoft.com/fwlink/?LinkID=722349&clcid=0x409";
}
