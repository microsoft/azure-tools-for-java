/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.microsoft.intellij.ui.messages.AzureBundle;
import com.wacommon.utils.WACommonException;

import java.io.File;

public class PluginHelper {

    private static final String AZURE_ARTIFACT = "azure-1.38.0.jar";

    /**
     * @return resource filename in plugin's directory
     */
    public static String getTemplateFile(String fileName) {
        return String.format("%s%s%s", PluginUtil.getPluginRootDirectory(), File.separator, fileName);
    }

    public static String getAzureLibLocation() throws WACommonException {
        String libLocation;
        try {
            String pluginInstLoc = PluginUtil.getPluginRootDirectory();
            libLocation = String.format(pluginInstLoc + "%s%s", File.separator, "lib");
            File file = new File(String.format(libLocation + "%s%s", File.separator, AZURE_ARTIFACT));
            if (!file.exists()) {
                throw new WACommonException(AzureBundle.message("SDKLocErrMsg"));
            }
        } catch (WACommonException e) {
            e.printStackTrace();
            throw e;
        }
        return libLocation;
    }
}
