/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ProjectSampleUtil {
    public static String getRootOrSourceFolder(Module module, boolean isSourceFolder) {
        ModuleRootManager moduleRootManager = module.getComponent(ModuleRootManager.class);
        if (module == null) {
            return null;
        }
        VirtualFile[] files = isSourceFolder ? moduleRootManager.getSourceRoots() : moduleRootManager.getContentRoots();

        if (files.length == 0) {
            DefaultLoader.getUIHelper().showError("Source Root should be created if you want to create a new sample project", "Create Sample Project");
            return null;
        }
        return files[0].getPath();
    }

    @NotNull
    private static String getNameFromPath(@NotNull String path) {
        int index = path.lastIndexOf('/');
        return path.substring(index);
    }

    public static void copyFileToPath(String[] resources, String toPath) throws Exception {
        for (int i = 0; i < resources.length; ++i) {
            File file = StreamUtil.getResourceFile(resources[i]);

            if (file == null) {
                DefaultLoader.getUIHelper().showError("Failed to get the sample resource folder for project", "Create Sample Project");
            } else {
                String toFilePath = StringHelper.concat(toPath, getNameFromPath(resources[i]));
                FileUtil.copy(file, new File(toFilePath));
            }
        }
    }
}
