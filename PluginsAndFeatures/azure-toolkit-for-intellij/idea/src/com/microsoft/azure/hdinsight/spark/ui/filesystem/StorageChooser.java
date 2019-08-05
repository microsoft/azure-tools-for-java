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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
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
        ApplicationManager.getApplication().invokeAndWait(() ->
                        PluginUtil.displayErrorDialog("Prepare Azure Virtual File System Error",
                                "Browsing files in the Azure virtual file system currently only supports ADLS Gen 2 " +
                                        "cluster. Please\n manually specify the reference file paths for other type of " +
                                        "clusters and check upload inputs. Or\n preparing upload path has a delay, please retry.")
                , ModalityState.any());
    }
}
