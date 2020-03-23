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

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JarUtils {

    private static final int BUFFER_SIZE = 4096;

    public static Path buildJarFileToStagingPath(String stagingFolder, Module module) throws IOException, AzureExecutionException {
        final File stagingFolderFile = new File(stagingFolder);
        if (!stagingFolderFile.exists()) {
            stagingFolderFile.mkdirs();
        }
        final String moduleName = module.getName();
        final String path = CompilerPaths.getModuleOutputPath(module, false);
        final Path outputFile = Paths.get(stagingFolder, moduleName + ".jar");
        try (final ZipOutputStream outputZip = getZipOutputStream(outputFile, true)) {
            zipDirectory(new File(path), "", outputZip);
            addManifest(outputZip);
        }
        return outputFile;
    }

    private static void zipDirectory(File folder, String parentFolder, ZipOutputStream zos)
            throws IOException {
        if (!folder.isDirectory()) {
            return;
        }
        final String prefix = StringUtils.isBlank(parentFolder) ? "" : (parentFolder + "/");
        for (final File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipDirectory(file, prefix + file.getName(), zos);
                continue;
            }
            zos.putNextEntry(new ZipEntry(prefix + file.getName()));
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                final byte[] bytesIn = new byte[BUFFER_SIZE];
                int read = 0;
                while ((read = bis.read(bytesIn)) != -1) {
                    zos.write(bytesIn, 0, read);
                }
            } finally {
                zos.closeEntry();
            }

        }
    }

    private static ZipOutputStream getZipOutputStream(Path outputFile, boolean override)
            throws AzureExecutionException, IOException {

        if (Files.exists(outputFile, LinkOption.NOFOLLOW_LINKS)) {
            if (override) {
                try {
                    Files.delete(outputFile);
                } catch (IOException exception) {
                    throw new AzureExecutionException("Removing \"" + outputFile + "\" failed. Not writing out anything.",
                            exception);
                }
            } else {
                throw new AzureExecutionException("Output file \"" + outputFile + "\" exists. Not overwriting.");
            }
        }
        return new ZipOutputStream(Files.newOutputStream(outputFile, StandardOpenOption.CREATE_NEW));

    }

    private static void addManifest(ZipOutputStream destination) throws IOException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "Azure Intellj Plugin");

        final ZipEntry manifestEntry = new ZipEntry("META-INF/MANIFEST.MF");
        destination.putNextEntry(manifestEntry);
        manifest.write(new BufferedOutputStream(destination));
        destination.closeEntry();
    }
}
