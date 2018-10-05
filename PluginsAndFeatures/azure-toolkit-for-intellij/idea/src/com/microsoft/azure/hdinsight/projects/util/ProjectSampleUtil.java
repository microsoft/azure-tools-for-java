/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
