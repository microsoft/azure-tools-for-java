/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.bicep.highlight;

import com.intellij.openapi.diagnostic.Logger;
import com.microsoft.azure.toolkit.intellij.bicep.activities.BicepStartupActivity;
import com.microsoft.azure.toolkit.lib.common.exception.SystemException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.microsoft.azure.toolkit.intellij.bicep.highlight.BicepEditorHighlighterProvider.LIB_ROOT;

public class ZipResourceUtils {
    protected static final Logger LOG = Logger.getInstance(BicepStartupActivity.class);

    @Nullable
    @AzureOperation("boundary/bicep.copy_textmate_bundle_from_jar")
    public static void copyTextMateBundlesFromJar(final String source, final String target) {
        final File targetFolder = new File(LIB_ROOT, target);
        if (!targetFolder.exists()) {
            final URL textmateZipUrl = ZipResourceUtils.class.getResource(source);
            if (textmateZipUrl != null) {
                try {
                    final File destZipFile = new File(LIB_ROOT, String.format("%s.zip", target));
                    FileUtils.copyURLToFile(textmateZipUrl, destZipFile);
                    unzip(destZipFile, targetFolder);
                } catch (IOException e) {
                    LOG.error(e);
                    targetFolder.delete();
                    AzureMessager.getMessager().error(e);
                }
            } else {
                throw new SystemException(String.format("'%s.zip' not found in toolkit", source));
            }
        }
    }

    @AzureOperation(value = "boundary/bicep.unzip_textmate_bundle.zip", params = "zipFilePath")
    private static void unzip(@Nonnull final File file, @Nonnull final File target) throws IOException {
        try (final java.util.zip.ZipFile zipFile = new ZipFile(file)) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final File entryDestination = new File(target, entry.getName());
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    try (final InputStream in = zipFile.getInputStream(entry);
                         final OutputStream out = new FileOutputStream(entryDestination)) {
                        IOUtils.copy(in, out);
                    }
                }
            }
        }
    }
}
