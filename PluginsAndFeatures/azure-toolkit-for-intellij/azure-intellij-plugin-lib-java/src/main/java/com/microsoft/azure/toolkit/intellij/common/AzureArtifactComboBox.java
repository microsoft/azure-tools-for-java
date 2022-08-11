/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;
import rx.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AzureArtifactComboBox extends AzureComboBox<AzureArtifact> {
    private final Project project;
    private final boolean fileArtifactOnly;
    private Condition<? super VirtualFile> fileFilter;
    private Subscription subscription;
    private AzureArtifact cachedArtifact;

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

    public void setArtifact(@Nullable final AzureArtifact azureArtifact) {
        final AzureArtifactManager artifactManager = AzureArtifactManager.getInstance(this.project);
        this.cachedArtifact = azureArtifact;
        Optional.ofNullable(cachedArtifact).filter(artifact -> artifact.getType() == AzureArtifactType.File).ifPresent(this::addItem);
        this.setValue(new ItemReference<>(artifact -> artifactManager.equalsAzureArtifact(cachedArtifact, artifact)));
    }

    @Override
    public AzureArtifact getValue() {
        if (value instanceof ItemReference && ((ItemReference<?>) value).is(cachedArtifact)) {
            return cachedArtifact;
        }
        return super.getValue();
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "common.list_artifacts.project",
        params = {"this.project.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends AzureArtifact> loadItems() throws Exception {
        final List<AzureArtifact> collect = fileArtifactOnly ?
            new ArrayList<>() : AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts();
        Optional.ofNullable(cachedArtifact).filter(artifact -> artifact.getType() == AzureArtifactType.File).ifPresent(collect::add);
        return collect;
    }

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
        final String tooltip = String.format("Open file (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final Extension openEx = Extension.create(AllIcons.General.OpenDisk, tooltip, this::onSelectFile);
        this.registerShortcut(keyStroke, openEx);
        return Collections.singletonList(openEx);
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

    @Nonnull
    public AzureValidationInfo doValidate(AzureArtifact artifact) {
        if (Objects.nonNull(artifact) && artifact.getType() == AzureArtifactType.File) {
            final VirtualFile referencedObject = (VirtualFile) artifact.getReferencedObject();
            if (Objects.nonNull(this.fileFilter) && !this.fileFilter.value(referencedObject)) {
                final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(this).message(AzureMessageBundle.message("common.artifact.artifactNotSupport").toString())
                    .type(Type.ERROR).build();
            }
        }
        return AzureValidationInfo.success(this);
    }

    private void onSelectFile() {
        final FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        if (fileFilter != null) {
            fileDescriptor.withFileFilter(fileFilter);
        }
        fileDescriptor.withTitle(AzureMessageBundle.message("common.artifact.selector.title").toString());
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
            artifacts.stream().filter(artifact -> manager.equalsAzureArtifact(artifact, selectArtifact)).findFirst().orElse(null);
        if (existingArtifact == null) {
            this.addItem(selectArtifact);
            this.setSelectedItem(selectArtifact);
        } else {
            this.setSelectedItem(existingArtifact);
        }
    }
}
