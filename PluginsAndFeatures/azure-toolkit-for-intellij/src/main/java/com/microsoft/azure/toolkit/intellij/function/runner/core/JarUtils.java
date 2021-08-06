/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.core;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
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
