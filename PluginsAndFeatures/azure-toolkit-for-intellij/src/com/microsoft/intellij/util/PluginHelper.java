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

package com.microsoft.intellij.util;

import com.microsoft.intellij.ui.messages.AzureBundle;
import com.wacommon.utils.WACommonException;

import java.io.File;

public class PluginHelper {

    private static final String AZURE_ARTIFACT = "azure-1.36.3.jar";

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
