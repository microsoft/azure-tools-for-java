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

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.ui.components.AzureArtifact;
import com.microsoft.intellij.ui.components.AzureArtifactManager;
import com.microsoft.intellij.ui.components.AzureArtifactType;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import rx.Subscription;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.microsoft.intellij.util.RxJavaUtils.unsubscribeSubscription;

public class AzureArtifactComboBox extends AzureComboBox<AzureArtifact> {
    private static final String ARTIFACT_NOT_SUPPORTED = "The selected platform does not support this type of artifact.";
    private final Project project;
    private final boolean fileArtifactOnly;
    private Condition<? super VirtualFile> fileFilter;
    private Subscription subscription;

    public AzureArtifactComboBox(Project project) {
        this(project, false);
    }

    public AzureArtifactComboBox(Project project, boolean fileArtifactOnly) {
        super(false);
        this.project = project;
        this.fileArtifactOnly = fileArtifactOnly;
    }

    public void setFileFilter(final Condition<? super VirtualFile> filter) {
        this.fileFilter = filter;
    }

    public synchronized void refreshItems(AzureArtifact defaultArtifact) {
        unsubscribeSubscription(subscription);
        this.setLoading(true);
        subscription = this.loadItemsAsync()
                           .subscribe(items -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                               this.setItems(items);
                               this.setLoading(false);
                               this.resetDefaultValue(defaultArtifact);
                           }), this::handleLoadingError);
    }

    @NotNull
    @Override
    protected List<? extends AzureArtifact> loadItems() throws Exception {
        return AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts()
                                   .stream()
                                   .filter(azureArtifact -> !fileArtifactOnly || azureArtifact.getType() == AzureArtifactType.File)
                                   .collect(Collectors.toList());
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(AllIcons.General.OpenDisk, "Open file", this::onSelectFile);
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

    @NotNull
    @Override
    public AzureValidationInfo doValidate() {
        final AzureValidationInfo info = super.doValidate();
        final AzureArtifact artifact = this.getValue();
        if (info == AzureValidationInfo.OK && Objects.nonNull(artifact) && artifact.getType() == AzureArtifactType.File) {
            final VirtualFile referencedObject = (VirtualFile) artifact.getReferencedObject();
            if (!this.fileFilter.value(referencedObject)) {
                final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(this).message(ARTIFACT_NOT_SUPPORTED).type(AzureValidationInfo.Type.ERROR).build();
            }
        }
        return info;
    }

    private void onSelectFile() {
        final FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        if (fileFilter != null) {
            fileDescriptor.withFileFilter(fileFilter);
        }
        fileDescriptor.withTitle("Select Artifact to Deploy");
        final VirtualFile file = FileChooser.chooseFile(fileDescriptor, null, null);
        if (file != null && file.exists()) {
            addOrSelectExistingVirtualFile(file);
        }
    }

    private void addOrSelectExistingVirtualFile(VirtualFile virtualFile) {
        final AzureArtifact selectArtifact = AzureArtifact.createFromFile(virtualFile);
        final List<AzureArtifact> artifacts = this.getItems();
        final AzureArtifactManager manager = AzureArtifactManager.getInstance(project);
        final AzureArtifact existingArtifact =
            artifacts.stream().filter(artifact -> manager.equalsAzureArtifactIdentifier(artifact, selectArtifact)).findFirst().orElse(null);
        if (existingArtifact == null) {
            this.addItem(selectArtifact);
            this.setSelectedItem(selectArtifact);
        } else {
            this.setSelectedItem(existingArtifact);
        }
    }

    private void resetDefaultValue(final AzureArtifact defaultArtifact) {
        if (defaultArtifact == null) {
            return;
        }
        final List<AzureArtifact> artifacts = this.getItems();
        final AzureArtifactManager manager = AzureArtifactManager.getInstance(project);
        final Predicate<AzureArtifact> predicate = artifact -> manager.equalsAzureArtifactIdentifier(defaultArtifact, artifact);
        final AzureArtifact toSelect = artifacts.stream().filter(predicate).findFirst().orElse(null);
        if (toSelect != null) {
            this.setSelectedItem(toSelect);
        } else if (defaultArtifact.getType() == AzureArtifactType.File) {
            this.addItem(defaultArtifact);
            this.setSelectedItem(defaultArtifact);
        } else {
            this.setSelectedItem(null);
        }
    }
}
