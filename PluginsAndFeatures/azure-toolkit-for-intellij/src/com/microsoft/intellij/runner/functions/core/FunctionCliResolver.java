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

package com.microsoft.intellij.runner.functions.core;

import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.common.utils.TextUtils;
import com.microsoft.azuretools.utils.CommandUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class FunctionCliResolver {
    private static final String RUNTIME_NOT_FOUND = "Azure Functions Core Tools not found. " +
            "Please go to https://aka.ms/azfunc-install to install Azure Functions Core Tools first.";

    public static String resolveFunc() throws IOException, InterruptedException {
        final boolean isWindows = CommandUtils.isWindows();
        final List<File> funCmdFiles = CommandUtils.resolvePathForCommand("func");
        File result = null;
        for (final File file : funCmdFiles) {
            final File canonicalFile = file.getCanonicalFile();
            if (!canonicalFile.exists()) {
                continue;
            }
            // when `func core tools` is manually installed and func is available at PATH
            // use canonical path to locate the real installation path
            result = findFuncExecInFolder(canonicalFile.getParentFile(), isWindows);
            if (result == null) {
                if (isWindows) {
                    result = resolveFuncForWindows(canonicalFile);
                } else {
                    // in linux/mac, when the way of `npm install azure-functions-core-tools`, the canonicalFile will point to `main.js`
                    if (canonicalFile.getName().equals("main.js")) {
                        result = findFuncExecInFolder(Paths.get(canonicalFile.getParent(), "..", "bin").normalize().toFile(),
                                isWindows);
                    }
                }
            }

            if (result != null) {
                return result.getAbsolutePath();
            }
        }
        Log.warn(TextUtils.red(RUNTIME_NOT_FOUND));
        return null;
    }

    private static File resolveFuncForWindows(final File canonicalFile) {
        if (canonicalFile.getName().equalsIgnoreCase("func.cmd")) {
            return findFuncExecInFolder(
                    Paths.get(canonicalFile.getParent(), "node_modules", "azure-functions-core-tools", "bin")
                            .toFile(),
                    true);
        } else {
            // check chocolate install
            final File libFolder = Paths
                    .get(canonicalFile.getParent(), "..", "lib", "azure-functions-core-tools", "tools")
                    .normalize().toFile();
            return findFuncExecInFolder(libFolder, true);
        }
    }

    private static File findFuncExecInFolder(final File folder, final boolean windows) {
        if (new File(folder, "func.dll").exists()) {
            final File func = new File(folder, windows ? "func.exe" : "func");
            if (func.exists()) {
                return func;
            }
        }
        return null;
    }

}
