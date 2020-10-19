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

package com.microsoft.azure.toolkit.intellij.appservice.component.input;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.ui.components.AzureArtifact;
import com.microsoft.intellij.ui.components.AzureArtifactManager;
import com.microsoft.intellij.ui.components.AzureArtifactType;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import rx.Subscription;

import javax.swing.*;
import java.util.List;

import static com.microsoft.intellij.util.RxJavaUtils.unsubscribeSubscription;

public class ComboBoxAzureArtifact extends AzureComboBox<AzureArtifact> {
    private Project project;
    private Condition<? super VirtualFile> filter;
    private Subscription subscription;

    public ComboBoxAzureArtifact(Project project) {
        super(false);
        this.project = project;
    }

    public void setFileChooserDescriptor(final Condition<? super VirtualFile> filter) {
        this.filter = filter;
    }

    public synchronized void refreshItems(AzureArtifactType defaultArtifactType, String defaultPath) {
        unsubscribeSubscription(subscription);
        this.setLoading(true);
        subscription = this.loadItemsAsync()
                           .subscribe(items -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                               this.removeAllItems();
                               this.setItems(items);
                               this.setLoading(false);
                               this.resetDefaultValue(defaultArtifactType, defaultPath);
                           }), (e) -> {
                               this.handleLoadingError(e);
                           });
    }

    @NotNull
    @Override
    protected List<? extends AzureArtifact> loadItems() throws Exception {
        return AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts();
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.OpenDisk, "Open file", this::onSelectFile);
    }

    protected String getItemText(Object item) {
        if (item instanceof AzureArtifact) {
            return String.format("%s : %s", ((AzureArtifact) item).getType(), ((AzureArtifact) item).getName());
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Nullable
    protected Icon getItemIcon(Object item) {
        return item instanceof AzureArtifact ? ((AzureArtifact) item).getIcon() : null;
    }

    private void onSelectFile() {
        final FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        if (filter != null) {
            fileDescriptor.withFileFilter(filter);
        }
        fileDescriptor.withTitle("Select artifact to deploy");
        final VirtualFile file = FileChooser.chooseFile(fileDescriptor, null, null);
        if (file != null && file.exists()) {
            addOrSelectExistingVirtualFile(file);
        }
    }

    private void addOrSelectExistingVirtualFile(VirtualFile virtualFile) {
        final AzureArtifact selectArtifact = AzureArtifact.createFromFile(virtualFile);
        final List<AzureArtifact> artifacts = UIUtils.listComboBoxItems(ComboBoxAzureArtifact.this);
        final AzureArtifact existingArtifact =
                artifacts.stream().filter(artifact -> artifact.getType() == AzureArtifactType.Artifact
                        && StringUtils.equalsAnyIgnoreCase(artifact.getTargetPath(), selectArtifact.getTargetPath()))
                         .findFirst().orElse(null);
        if (existingArtifact == null) {
            ComboBoxAzureArtifact.this.addItem(selectArtifact);
            ComboBoxAzureArtifact.this.setSelectedItem(selectArtifact);
        } else {
            ComboBoxAzureArtifact.this.setSelectedItem(existingArtifact);
        }
    }

    private void resetDefaultValue(final AzureArtifactType defaultArtifactType, final String defaultPath) {
        final List<AzureArtifact> artifacts = UIUtils.listComboBoxItems(this);
        final AzureArtifact defaultArtifact =
                artifacts.stream().filter(artifact -> StringUtils.equals(artifact.getTargetPath(), defaultPath)
                        && defaultArtifactType == artifact.getType())
                         .findFirst().orElse(null);
        if (defaultArtifact != null) {
            this.setSelectedItem(defaultArtifact);
        } else if (defaultArtifactType == AzureArtifactType.File) {
            AzureArtifact userArtifact =
                    AzureArtifact.createFromFile(LocalFileSystem.getInstance().findFileByPath(defaultPath));
            this.addItem(userArtifact);
            this.setSelectedItem(userArtifact);
        } else {
            this.setSelectedItem(null);
        }
    }
}
