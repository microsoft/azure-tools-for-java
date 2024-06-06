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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AzureArtifactComboBox extends AzureComboBox<AzureArtifact> {
    private final Project project;
    @Getter
    @Setter
    private boolean fileArtifactOnly;
    @Nonnull
    private Predicate<? super VirtualFile> fileFilter = artifact -> true;
    @Nonnull
    private Predicate<? super AzureArtifact> artifactFilter = artifact -> true;
    private AzureArtifact cachedArtifact;

    public AzureArtifactComboBox(Project project) {
        this(project, false);
    }

    public AzureArtifactComboBox(Project project, boolean fileArtifactOnly) {
        super(false);
        this.project = project;
        this.fileArtifactOnly = fileArtifactOnly;
        this.setRenderer(new ArtifactItemRenderer());
    }

    public void setFileFilter(@Nonnull final Predicate<? super VirtualFile> filter) {
        this.fileFilter = filter;
    }

    public void setArtifactFilter(@Nonnull final Predicate<? super AzureArtifact> filter) {
        this.artifactFilter = filter;
    }

    public void setArtifact(@Nullable final AzureArtifact azureArtifact) {
        final AzureArtifactManager artifactManager = AzureArtifactManager.getInstance(this.project);
        this.cachedArtifact = azureArtifact;
        Optional.ofNullable(cachedArtifact).filter(artifact -> artifact.getType() == AzureArtifactType.File).ifPresent(this::addItem);
        this.reloadItems();
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
    @AzureOperation(name = "internal/common.list_artifacts.project", params = {"this.project.getName()"})
    protected List<? extends AzureArtifact> loadItems() {
        final List<AzureArtifact> collect = fileArtifactOnly ?
            new ArrayList<>() : AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts().stream().filter(this.artifactFilter).collect((Collectors.toCollection(ArrayList::new)));
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
            final Integer level = ((AzureArtifact) item).getBytecodeTargetLevel();
            final String version = level > 7 ? "Java " + level : "Java 1." + level;
            return String.format("%s: %s (%s)", ((AzureArtifact) item).getType(), ((AzureArtifact) item).getName(), version);
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
            if (!this.fileFilter.test(referencedObject)) {
                final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(this).message(AzureMessageBundle.message("common.artifact.artifactNotSupport").toString())
                    .type(Type.ERROR).build();
            }
        }
        return AzureValidationInfo.success(this);
    }

    private void onSelectFile() {
        final FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        fileDescriptor.withFileFilter(v -> this.fileFilter.test(v));
        fileDescriptor.withTitle(AzureMessageBundle.message("common.artifact.selector.title").toString());
        final VirtualFile file = FileChooser.chooseFile(fileDescriptor, null, null);
        if (file != null && file.exists()) {
            addOrSelectExistingVirtualFile(file);
        }
    }

    private void addOrSelectExistingVirtualFile(VirtualFile virtualFile) {
        final AzureArtifact selectArtifact = AzureArtifact.createFromFile(virtualFile, this.project);
        final List<AzureArtifact> artifacts = this.getItems();
        final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.project);
        final AzureArtifact existingArtifact =
            artifacts.stream().filter(artifact -> manager.equalsAzureArtifact(artifact, selectArtifact)).findFirst().orElse(null);
        if (existingArtifact == null) {
            this.addItem(selectArtifact);
            this.setSelectedItem(selectArtifact);
        } else {
            this.setSelectedItem(existingArtifact);
        }
    }

    public void setModule(Module module) {
        this.artifactFilter = artifact -> artifact.getModule() == module;
    }

    public static class ArtifactItemRenderer extends ColoredListCellRenderer<AzureArtifact> {

        @Override
        protected void customizeCellRenderer(@Nonnull JList<? extends AzureArtifact> list, AzureArtifact artifact, int index, boolean selected, boolean hasFocus) {
            if (artifact != null) {
                final Integer level = artifact.getBytecodeTargetLevel();
                setIcon(artifact.getIcon());
                append(artifact.getName().trim());
                if (level != null) {
                    append(level > 7 ? " Java " + level : " Java 1." + level, SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
                }
            }
        }
    }
}
