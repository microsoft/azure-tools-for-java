/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.function;

import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.microsoft.azure.toolkit.lib.common.utils.CommandUtils.isWindows;

public abstract class FunctionCoreToolsResolver {
    @AzureOperation(name = "function.resolve_cli", type = AzureOperation.Type.TASK)
    public static List<String> resolve() {
        final FunctionCoreToolsResolver resolver = CommandUtils.isWindows() ? new WindowsFunctionCoreToolsResolver() : new LinuxFunctionCoreToolsResolver();
        return resolver.resolveInner();
    }

    @Cacheable(cacheName = "function/resolve-cli")
    private List<String> resolveInner() {
        // resolve command from $PATH
        final List<String> whichFuncDirs = resolveCommandPath("func");
        final Set<String> results = new HashSet<>();
        final Set<String> processedDirectories = new HashSet<>();
        for (final String dir : whichFuncDirs) {
            try {
                final File canonicalFile = new File(dir).getCanonicalFile();
                if (!canonicalFile.exists()) {
                    continue;
                }
                final String parentFolder = canonicalFile.getParentFile().getAbsolutePath();
                if (!processedDirectories.add(parentFolder)) {
                    // already processed
                    continue;
                }
                // when `func core tools` is manually installed and func is available at PATH
                // use canonical path to locate the real installation path
                String result = findFuncInFolder(parentFolder);
                if (result == null) {
                    result = resolveFuncSystemSpecific(parentFolder);
                }
                if (result != null) {
                    results.add(result);
                }

            } catch (IOException ignored) {
                // ignore
            }
        }
        Optional.ofNullable(findFuncInNpm()).ifPresent(results::add);
        return new ArrayList<>(results);
    }

    @Nullable
    protected String findFuncInFolder(final String parentFolder) {
        if (new File(parentFolder, "func.dll").exists()) {
            final File func = new File(parentFolder, getFuncFileName());
            if (func.exists()) {
                return Paths.get(func.getAbsolutePath()).normalize().toString();
            }
        }
        return null;
    }

    @Nullable
    protected String findFuncInNpm() {
        try {
            final String output = StringUtils.trim(CommandUtils.exec("npm root --global"));
            final File path = new File(output, "azure-functions-core-tools/bin");
            if (FileUtils.isDirectory(path)) {
                return findFuncInFolder(path.getAbsolutePath());
            }

        } catch (IOException ignore) {
            // ignore
        }
        return null;
    }

    private static List<String> resolveCommandPath(String command) {
        final List<String> list = new ArrayList<>();
        try {

            final String output = CommandUtils.exec((isWindows() ? "where " : "which ") + command);
            if (StringUtils.isBlank(output)) {
                return Collections.emptyList();
            }

            for (final String outputLine : output.split("[\\r\\n]")) {
                final File file = new File(StringUtils.trim(outputLine));
                if (!file.exists() || !file.isFile()) {
                    continue;
                }

                list.add(file.getAbsolutePath());
            }
        } catch (IOException ignored) {
            // ignore
        }
        return list;
    }

    abstract String getFuncFileName();

    abstract String resolveFuncSystemSpecific(final String funcParentFolder);

    static class WindowsFunctionCoreToolsResolver extends FunctionCoreToolsResolver {

        @Override
        String getFuncFileName() {
            return "func.exe";
        }

        @Override
        String resolveFuncSystemSpecific(String funcParentFolder) {
            // handle the case of choco install:
            // from C:\ProgramData\chocolatey\bin\func.exe -> C:\ProgramData\chocolatey\lib\azure-functions-core-tools\tools\func.exe
            return findFuncInFolder(Paths.get(funcParentFolder, "..\\lib\\azure-functions-core-tools\\tools").toString());
        }
    }

    static class LinuxFunctionCoreToolsResolver extends FunctionCoreToolsResolver {
        @Override
        String getFuncFileName() {
            return "func";
        }

        @Override
        String resolveFuncSystemSpecific(final String funcParentFolder) {
            // detect func installed by `brew install azure-functions-core-tools@3`
            // readlink /usr/local/bin/func =>
            //../Cellar/azure-functions-core-tools@3/3.0.3477/bin/func
            return findFuncInFolder(Paths.get(funcParentFolder, "../bin").toString());
        }
    }

}
