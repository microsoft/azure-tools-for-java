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
package com.microsoft.intellij.ui.libraries;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class AddLibraryUtility {
    static final String FILE_SUFFIX = ".jar";

    static void addLibraryRoot(File file, Library.ModifiableModel libraryModel) {
        if (file.isFile()) {
            libraryModel.addRoot(VfsUtil.getUrlForLibraryRoot(file), OrderRootType.CLASSES);
        } else {
            for (File file0 : file.listFiles()) {
                addLibraryRoot(file0, libraryModel);
            }
        }
    }

    static void addLibraryFiles(File file, Library.ModifiableModel libraryModel, String[] files) {
        List filesList = Arrays.asList(files);
        for (File file0 : file.listFiles()) {
            if (filesList.contains(extractArtifactName(file0.getName()).toLowerCase())) {
                addLibraryRoot(file0, libraryModel);
            }
        }
    }

    static String extractArtifactName(String nameWithVersion) {
        if (nameWithVersion == null) {
            return "";
        }
        nameWithVersion = nameWithVersion.trim();
        if (!nameWithVersion.endsWith(FILE_SUFFIX)){
            return nameWithVersion;
        }

        nameWithVersion =  nameWithVersion.substring(0, nameWithVersion.length() - FILE_SUFFIX.length());
        int index = nameWithVersion.indexOf('.');
        if (index < 0) {
            return nameWithVersion;
        }

        String artifactName = nameWithVersion;

        int lastIndex = nameWithVersion.lastIndexOf('-');
        while (lastIndex > index) {
            nameWithVersion = nameWithVersion.substring(0, lastIndex);
            lastIndex = nameWithVersion.lastIndexOf('-');
        }

        if (lastIndex < 0) {
            return artifactName;
        }

        artifactName = nameWithVersion.substring(0, lastIndex);
        return artifactName;
    }
}
