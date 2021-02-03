/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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

        nameWithVersion = nameWithVersion.substring(0, nameWithVersion.length() - FILE_SUFFIX.length());
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
