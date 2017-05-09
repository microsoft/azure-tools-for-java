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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryProperties;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SparkLibraryType extends LibraryType<SparkLibraryProperties>{

    // overriding deprecated method to temporarily make code compatible both with IntelliJ 2016.x and 2017.1
    @Nullable
    @Override
    public Icon getIcon() {
        return PlatformIcons.LIBRARY_ICON;
    }

//    public Icon getIcon(@Nullable SparkLibraryProperties properties) {
//        return PlatformIcons.LIBRARY_ICON;
//    }

    public SparkLibraryType() {
        super(SparkLibraryKind.getInstance());
    }
    @Nullable
    @Override
    public String getCreateActionName() {
        return "Spark SDK";
    }

    public static SparkLibraryType getInstance() {
        return LibraryType.EP_NAME.findExtension(SparkLibraryType.class);
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent jComponent, @Nullable VirtualFile virtualFile, @NotNull Project project) {
        return (new SparkLibraryDescription()).createNewLibrary(jComponent, virtualFile);
    }

    @Nullable
    @Override
    public LibraryPropertiesEditor createPropertiesEditor(@NotNull LibraryEditorComponent<SparkLibraryProperties> libraryEditorComponent) {
        return null;
    }
}
