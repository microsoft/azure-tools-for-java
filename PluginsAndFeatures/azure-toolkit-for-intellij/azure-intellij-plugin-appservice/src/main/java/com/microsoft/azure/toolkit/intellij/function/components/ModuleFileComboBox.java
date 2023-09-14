/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.ProjectUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ModuleFileComboBox extends AzureComboBox<VirtualFile> {
    public static final VirtualFile EMPTY = LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home"));
    private final String fileName;
    private Module module;
    private final @Nonnull Project project;
    private final List<VirtualFile> drafts = new ArrayList<>();

    public ModuleFileComboBox(@Nonnull final Project project, @Nonnull final String fileName) {
        super(false);
        this.fileName = fileName;
        this.project = project;
        refreshItems();
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Select exising file from disk (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.OpenDisk, tooltip, this::selectLocalSettings);
        this.registerShortcut(keyStroke, addEx);
        return Collections.singletonList(addEx);
    }

    private void selectLocalSettings() {
        final String extension = FileNameUtils.getExtension(fileName);
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(extension);
        descriptor.withTitle("Select Path for File");
        final VirtualFile current = getValue();
        final VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, getValue());
        if (ArrayUtils.isNotEmpty(files)) {
            this.drafts.add(files[0]);
            this.reloadItems();
            this.setValue(files[0]);
        }
    }

    @Nonnull
    @Override
    protected List<? extends VirtualFile> loadItems() throws Exception {
        final GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        final Collection<VirtualFile> localFiles = ReadAction.compute(() -> FilenameIndex.getVirtualFilesByName(fileName, scope)).stream()
                .filter(file -> module == null || ProjectUtils.isSameModule(ModuleUtil.findModuleForFile(file, project), module)).collect(Collectors.toList());
        return ListUtils.union(new ArrayList<>(localFiles), drafts);
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof VirtualFile ? FilenameUtils.separatorsToSystem(((VirtualFile) item).getCanonicalPath()) : super.getItemText(item);
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object item) {
        return item instanceof VirtualFile ? AllIcons.FileTypes.Json : super.getItemIcon(item);
    }

    public void setModule(final Module module) {
        if (Objects.equals(module, this.module)) {
            return;
        }
        this.module = module;
        refreshItems();
    }
}
