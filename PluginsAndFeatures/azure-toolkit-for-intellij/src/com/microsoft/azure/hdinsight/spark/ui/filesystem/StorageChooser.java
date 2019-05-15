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
import com.intellij.util.ArrayUtil;
import com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.ADLSGen2FSOperation;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class StorageChooser {
    public static StorageChooser instance = new StorageChooser(null);
    private ADLSGen2FileSystem fileSystem;

    private StorageChooser(@Nullable ADLSGen2FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public void setFileSystem(ADLSGen2FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public ADLSGen2FileSystem getFileSystem() {
        return this.fileSystem;
    }

    public List<VirtualFile> setRoots() {
        if (fileSystem == null) {
            return Arrays.asList(AdlsGen2VirtualFile.empty);
        }

        String rootPath = fileSystem.rootPath.replace("/SparkSubmission/", "");
        HashMap<String, AdlsGen2VirtualFile> parentFiles = new HashMap<>();
        AdlsGen2VirtualFile root = new AdlsGen2VirtualFile("SparkSubmission", true, rootPath, fileSystem);
        parentFiles.put(root.getPath(), root);
        ADLSGen2FSOperation op = new ADLSGen2FSOperation(fileSystem.http);
        op.list(rootPath)
                .map(path -> new AdlsGen2VirtualFile(path.getName(), path.isDirectory(), rootPath, fileSystem))
                .doOnNext(file -> {
                    if (file.isDirectory()) {
                        parentFiles.put(file.getPath(), file);
                    }

                    String parentPath = file.getPath().substring(0, file.getPath().lastIndexOf("/"));
                    if (parentFiles.keySet().contains(parentPath)) {
                        parentFiles.get(parentPath).addChildren(file);
                        file.setParent(parentFiles.get(parentPath));
                    }
                }).toBlocking().subscribe(ob -> {
        });

        return Arrays.asList(root);
    }

    public VirtualFile chooseFile(FileChooserDescriptor descriptor) {
        Component parentComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        final FileChooserDialog chooser = new StorageChooserDialogImpl(descriptor, parentComponent, null);
        return ArrayUtil.getFirstElement(chooser.choose(null, new AdlsGen2VirtualFile[]{null}));
    }
}
