/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.util.PluginUtil;

import java.awt.*;

public class StorageChooser implements ILogger {
    public static final Condition<VirtualFile> ALL_DIRS_AND_FILES = (vf) -> true;
    FileChooserDescriptor descriptor;
    AzureStorageVirtualFile root;

    public StorageChooser(@Nullable AzureStorageVirtualFile vf, Condition<VirtualFile> filter) {
        descriptor = new FileChooserDescriptor(true, false, true, false, false, true)
                .withFileFilter(filter);
        root = vf;
        descriptor.setRoots(vf);
    }

    public VirtualFile[] chooseFile() {
        Component parentComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        final FileChooserDialog chooser = new StorageChooserDialogImpl(this.descriptor, parentComponent, null);
        return chooser.choose(null, this.root);
    }

    public static void handleInvalidUploadInfo() {
        AzureTaskManager.getInstance().runAndWait(() ->
                        PluginUtil.displayErrorDialog("Prepare Azure Virtual File System Error",
                                "Browsing files in the Azure virtual file system currently only supports ADLS Gen 2 " +
                                        "cluster. Please\n manually specify the reference file paths for other type of " +
                                        "clusters and check upload inputs. Or\n preparing upload path has a delay, please retry."), AzureTask.Modality.ANY);
    }

    public static void handleListChildrenFailureInfo(String errorMessage) {
        AzureTaskManager.getInstance().runAndWait(() -> PluginUtil.displayErrorDialog("List files failure", errorMessage), AzureTask.Modality.ANY);
    }
}
