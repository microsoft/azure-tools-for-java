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
package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.roots.impl.libraries.LibraryTypeServiceImpl;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.DefaultLibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class SparkLibraryDescription extends CustomLibraryDescription {

    private String localPath;

    @NotNull
    public String getLocalPath() {
        return localPath;
    }

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return Collections.singleton(SparkLibraryKind.getInstance());
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent jComponent, @Nullable VirtualFile virtualFile) {
        return getSparkSDKConfigurationFromLocalFile();
    }

    private NewLibraryConfiguration getSparkSDKConfigurationFromLocalFile() {
        FileChooserDescriptor chooserDescriptor = new FileChooserDescriptor(false, false, true, false, true, false);
        chooserDescriptor.setTitle("Select Spark SDK");

        String pluginPath = PluginUtil.getPluginRootDirectory();

        VirtualFile pluginVfs = LocalFileSystem.getInstance().findFileByPath(pluginPath);

        VirtualFile chooseFile = FileChooser.chooseFile(chooserDescriptor, null, pluginVfs);
        if (chooseFile == null) {
            return null;
        }
        this.localPath = chooseFile.getPath();

        final List<OrderRoot> roots = RootDetectionUtil.detectRoots(Arrays.asList(chooseFile), null, null, new DefaultLibraryRootsComponentDescriptor());

        if (roots.isEmpty()) {
            return null;
        }

        return new NewLibraryConfiguration(LibraryTypeServiceImpl.suggestLibraryName(roots), SparkLibraryType.getInstance(), new SparkLibraryProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor libraryEditor) {
                libraryEditor.addRoots(roots);
            }
        };
    }
}
