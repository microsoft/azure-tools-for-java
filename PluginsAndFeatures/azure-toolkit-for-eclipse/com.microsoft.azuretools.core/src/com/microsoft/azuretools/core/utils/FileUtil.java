/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;

import com.microsoft.azuretools.core.Activator;


public class FileUtil {

    /**
     * copy specified file to eclipse plugins folder
     * @param name : Name of file
     * @param entry : Location of file
     */
    public static void copyResourceFile(String resourceFile , String destFile) {
        URL url = Activator.getDefault().getBundle()
                .getEntry(resourceFile);
        URL fileURL;
        try {
            fileURL = FileLocator.toFileURL(url);
            URL resolve = FileLocator.resolve(fileURL);
            File file = new File(resolve.getFile());
            FileInputStream fis = new FileInputStream(file);
            File outputFile = new File(destFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            com.microsoft.azuretools.azurecommons.util.FileUtil.writeFile(fis , fos);
        } catch (IOException e) {
            Activator.getDefault().log(e.getMessage(), e);
        }

    }
}
