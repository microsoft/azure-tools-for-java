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

package com.microsoft.azure.toolkit.intellij.appservice.artifact;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ArtifactComboBox extends AzureComboBox<Path> {

    public static final String FILE_CHOOSER_TITLE = "Select Artifact to Deploy";

    @NotNull
    @Override
    protected List<? extends Path> loadItems() throws Exception {
        final Path[] paths = {
                Paths.get("C:\\Users\\wangmi\\workspace\\azure-tools-for-java"),
                Paths.get("C:\\Users\\wangmi\\workspace\\azure-maven-plugins")
        };
        return Arrays.asList(paths);
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object o) {
        if (o instanceof VirtualFile) {
            return AllIcons.FileTypes.Archive;
        } else {
            return super.getItemIcon(o);
        }
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.OpenDisk, "Open file", this::onSelectFile);
    }

    private void onSelectFile() {
        // Todo: enable customize file descriptor
        final FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        fileDescriptor.setTitle(FILE_CHOOSER_TITLE);
        final VirtualFile file = FileChooser.chooseFile(fileDescriptor, null, null);
        if (Objects.nonNull(file) && file.exists()) {
            addOrSelectExistingVirtualFile(file);
        }
    }

    private void addOrSelectExistingVirtualFile(VirtualFile virtualFile) {
        for (int i = 0; i < ArtifactComboBox.this.getItemCount(); i++) {
            final Object object = ArtifactComboBox.this.getItemAt(i);
            if (object instanceof VirtualFile && StringUtils.equals(virtualFile.getPath(),
                                                                    ((VirtualFile) object).getPath())) {
                ArtifactComboBox.this.setSelectedItem(virtualFile);
                return;
            }
        }
        ArtifactComboBox.this.addItem(Paths.get(virtualFile.getPath()));
        ArtifactComboBox.this.setSelectedItem(virtualFile);
    }

}
