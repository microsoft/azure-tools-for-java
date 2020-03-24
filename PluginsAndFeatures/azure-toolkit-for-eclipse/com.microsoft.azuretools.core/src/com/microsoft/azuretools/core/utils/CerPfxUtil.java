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

package com.microsoft.azuretools.core.utils;

import java.io.File;
import java.security.cert.X509Certificate;

import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;


/**
 * Class has utility methods to work with certificate files .cer and .pfx
 */
public class CerPfxUtil {

    /**
     * Method opens file dialog to accept .cer or .pfx file.
     *
     * @param shell
     * @param path
     * @return
     */
    public static String importCerPfx(Shell shell, String path) {
        FileDialog dialog = new FileDialog(shell);
        String[] extensions = {"*.cer", "*.CER", "*.pfx", "*.PFX"};
        dialog.setFilterExtensions(extensions);
        dialog.setText("Select Certificate");
        // Default directory should be the cert directory in the project, and if
        // it
        // doesn't exist, then it should be the project directory
        String certPath = path + File.separator + "cert";
        if (new File(certPath).exists()) {
            dialog.setFilterPath(certPath);
        } else {
            dialog.setFilterPath(path);
        }
        String file = dialog.open();
        if (file != null) {
            file.replace('\\', '/');
        }
        return file;
    }

    /**
     * Method returns X509Certificate certificate object.
     *
     * @param certURL
     * @return
     */
    public static X509Certificate getCert(String certURL, String password) {
        return com.microsoft.azuretools.azurecommons.util.CerPfxUtil.getCert(certURL, password);
    }

    /**
     * This method resolves environment variables in path. Format of env
     * variables is ${env.VAR_NAME} e.g. ${env.JAVA_HOME}
     *
     * @param rawPath
     * @return
     */
    public static String getCertificatePath(String rawPath) {
        return com.microsoft.azuretools.azurecommons.util.CerPfxUtil.getCertificatePath(rawPath);
    }
}
