/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class StorageChooserDialogImpl extends FileChooserDialogImpl {
    private FileChooserDescriptor myChooserDescriptor;
    private VirtualFile[] myChosenFiles = VirtualFile.EMPTY_ARRAY;

    public StorageChooserDialogImpl(@NotNull FileChooserDescriptor descriptor, @NotNull Component parent, @Nullable Project project) {
        super(descriptor, parent, project);
        this.myChooserDescriptor = descriptor;
    }

    @Override
    protected void doOKAction() {
        if (!isOKActionEnabled()) {
            return;
        }

        if (isTextFieldActive()) {
            final String text = myPathTextField.getTextFieldText();
            if (text == null) {
                setErrorText("Specified path cannot be found", myPathTextField.getField());
                return;
            }
        }

        myChosenFiles = myFileSystemTree.getSelectedFiles();
        if (getOKAction().isEnabled()) {
            // FIXME: Must update operation name if VFS for Blob or Gen1 are supported in the future
            EventUtil.logEvent(EventType.info, TelemetryConstants.VFS, TelemetryConstants.CHOOSE_REFERENCE_JAR_GEN2, null);

            close(OK_EXIT_CODE);
        }
    }

    @Override
    @NotNull
    public VirtualFile[] choose(@Nullable final Project project, @NotNull final VirtualFile... toSelect) {
        super.choose(project, toSelect);
        return myChosenFiles;
    }

    private boolean isTextFieldActive() {
        return myPathTextField.getField().getRootPane() != null;
    }
}
