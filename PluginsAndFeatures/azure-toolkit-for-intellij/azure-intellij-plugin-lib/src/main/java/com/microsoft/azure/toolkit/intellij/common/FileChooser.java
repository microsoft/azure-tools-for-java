/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.File;

public class FileChooser {

    @Nullable
    public static File showFileSaver(String title, String fileName) {
        return showFileSaver(title, fileName, StringUtils.EMPTY);
    }

    @Nullable
    public static File showFileSaver(String title, String fileName, String description) {
        final FileSaverDescriptor fileDescriptor = new FileSaverDescriptor(title, description);
        final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileDescriptor, (Project) null);
        final VirtualFileWrapper save = dialog.save(LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")), fileName);
        if (save != null) {
            return save.getFile();
        }
        return null;
    }
}
