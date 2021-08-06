/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.core;

import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azuretools.utils.CommandUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FunctionCliResolver {
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
        Log.warn(TextUtils.red(message("function.cli.error.notFound")));
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
