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
package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.ADLSGen2FSOperation;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.awt.*;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class StorageChooser implements ILogger {
    private AzureStorageVirtualFileSystem fileSystem;

    public StorageChooser(@Nullable AzureStorageVirtualFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public List<VirtualFile> setRoots(String uploadRootPath) {
        if (fileSystem == null) {
            return Arrays.asList(AdlsGen2VirtualFile.Empty);
        }

        String rootPath = uploadRootPath.replace("/SparkSubmission/", "");

        // key: path without prefix , value: virtual file (directory type)
        HashMap<String, AzureStorageVirtualFile> parentFiles = new HashMap<>();
        AdlsGen2VirtualFile root = new AdlsGen2VirtualFile("SparkSubmission", true, rootPath, fileSystem);
        parentFiles.put(URI.create(root.getPath()).getPath(), root);

        fileSystem.getAzureStorageVirtualFiles(rootPath)
                .doOnNext(file -> {
                    String fileNameWithoutPrefix = URI.create(file.getPath()).getPath();
                    if (file.isDirectory()) {
                        parentFiles.put(fileNameWithoutPrefix, file);
                    }

                    String parentPath = Paths.get(fileNameWithoutPrefix).getParent().toString();
                    if (parentFiles.keySet().contains(parentPath)) {
                        parentFiles.get(parentPath).addChildren(file);
                        file.setParent(parentFiles.get(parentPath));
                    }
                }).toBlocking().subscribe(ob -> {
                },
                err -> {
                    log().warn("Listing files encounters exception", err);
                });

        return Arrays.asList(root);
    }

    public VirtualFile[] chooseFile(FileChooserDescriptor descriptor) {
        Component parentComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        final FileChooserDialog chooser = new StorageChooserDialogImpl(descriptor, parentComponent, null);
        return chooser.choose(null, new AdlsGen2VirtualFile[]{null});
    }
}
