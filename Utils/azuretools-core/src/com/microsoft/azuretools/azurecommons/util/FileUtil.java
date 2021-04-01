/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.util;

import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtil {

    private static final int BUFF_SIZE = 1024;
    private static final String USER_HOME = "user.home";

    /**
     * Method writes contents of file.
     * @param inStream
     * @param outStream
     * @throws IOException
     */
    public static void writeFile(InputStream inStream, OutputStream outStream)
            throws IOException {

        try {
            byte[] buf = new byte[BUFF_SIZE];
            int len = inStream.read(buf);
            while (len > 0) {
                outStream.write(buf, 0, len);
                len = inStream.read(buf);
            }
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (outStream != null) {
                outStream.close();
            }
        }
    }

    /**
     * Copies jar file from zip
     *
     * @throws IOException
     */
    public static boolean copyFileFromZip(File zipResource, String fileName, File destFile) throws IOException {
        boolean success = false;

        ZipFile zipFile = new ZipFile(zipResource);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        File destParentFile = destFile.getParentFile();

        // create parent directories if not existing
        if (destFile != null && destParentFile != null && !destParentFile.exists()) {
            destParentFile.mkdir();
        }

        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.getName().equals(fileName)) {
                writeFile(zipFile.getInputStream(zipEntry), new BufferedOutputStream(new FileOutputStream(destFile)));
                success = true;
                break;
            }
        }
        zipFile.close();

        return success;
    }

    /**
     * Utility method to check for null conditions or empty strings.
     * @param name
     * @return true if null or empty string
     */
    public static boolean isNullOrEmpty(final String name) {
        boolean isValid = false;
        if (name == null || name.matches("\\s*")) {
            isValid = true;
        }
        return isValid;
    }

    public static void addToZipFile(@NotNull final File file, @NotNull final ZipOutputStream zos) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        try {
            final ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            final byte[] bytes = new byte[BUFF_SIZE];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        } finally {
            zos.closeEntry();
            fis.close();
        }
    }

    private static void ensureValidZipSourceAndTarget(@NotNull final File[] sourceFiles,
                                                      @NotNull final File targetZipFile) throws Exception {
        final String targetZipFileName = targetZipFile.getName();
        final String targetZipFileExtension = targetZipFileName.substring(targetZipFileName.lastIndexOf(".") + 1);
        if (!targetZipFileExtension.equalsIgnoreCase("zip")) {
            throw new Exception("The target file should be a .zip file.");
        }

        for (final File file : sourceFiles) {
            if (!file.exists()) {
                throw new Exception(String.format("The source file: %s does not exist.", file.getName()));
            }
        }
    }

    /**
     * Utility method to zip the given source file to the destination file.
     * @param sourceFile source file
     * @param targetZipFile ZIP file that will be created or overwritten
     */
    public static void zipFile(@NotNull final File sourceFile, @NotNull final File targetZipFile) throws Exception {
        zipFiles(new File[] {sourceFile}, targetZipFile);
    }

    /**
     * Utility method to zip the given collection source files to the destination file.
     * @param sourceFiles source files array
     * @param targetZipFile ZIP file that will be created or overwritten
     */
    @AzureOperation(
        name = "common|artifact.zip_files",
        params = {"$targetZipFile.getName()"},
        type = AzureOperation.Type.TASK
    )
    public static void zipFiles(@NotNull final File[] sourceFiles,
                                @NotNull final File targetZipFile) throws Exception {
        ensureValidZipSourceAndTarget(sourceFiles, targetZipFile);
        final FileOutputStream fos = new FileOutputStream(targetZipFile);
        final ZipOutputStream zipOut = new ZipOutputStream(fos);
        try {
            for (final File file : sourceFiles) {
                addToZipFile(file, zipOut);
            }
        } finally {
            if (zipOut != null) {
                zipOut.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static boolean isNonEmptyFolder(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.isDirectory() && file.listFiles() != null && file.listFiles().length > 0;
    }

    public static Path getDirectoryWithinUserHome(String folder) {
        return Paths.get(System.getProperty(USER_HOME), folder);
    }
}
