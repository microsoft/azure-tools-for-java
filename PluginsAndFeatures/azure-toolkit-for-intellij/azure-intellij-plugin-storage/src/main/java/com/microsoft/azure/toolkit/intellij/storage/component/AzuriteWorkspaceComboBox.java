/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.storage.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.storage.azurite.AzuriteService;
import com.microsoft.azure.toolkit.lib.Azure;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AzuriteWorkspaceComboBox extends AzureComboBox<String> {

    private final List<String> draftPaths = new ArrayList<>();

    @Nullable
    @Override
    protected Icon getItemIcon(Object item) {
        if (item instanceof String) {
            final String value = (String) item;
            switch (value) {
                case AzuriteService.INTELLIJ_GLOBAL_STORAGE:
                case AzuriteService.CURRENT_PROJECT:
                    return AllIcons.Nodes.IdeaProject;
                default:
                    return AllIcons.FileTypes.Archive;
            }
        }
        return super.getItemIcon(item);
    }

    @Override
    public void setValue(String value) {
        if (!StringUtils.equalsAnyIgnoreCase(value, AzuriteService.INTELLIJ_GLOBAL_STORAGE, AzuriteService.CURRENT_PROJECT) && FileUtil.exists(value)) {
            draftPaths.add(value);
        }
        super.setValue(value);
    }

    @Override
    public List<String> loadItems() {
        final String azuritePath = Azure.az().config().getAzuritePath();
        final List<String> values = Arrays.asList(AzuriteService.INTELLIJ_GLOBAL_STORAGE, AzuriteService.CURRENT_PROJECT, azuritePath);
        return Stream.of(values, draftPaths).flatMap(List::stream).filter(StringUtils::isNoneBlank).distinct().collect(Collectors.toList());
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
        final String tooltip = String.format("Open file (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension openEx = ExtendableTextComponent.Extension.create(AllIcons.General.OpenDisk, tooltip, this::onSelectFile);
        this.registerShortcut(keyStroke, openEx);
        return Collections.singletonList(openEx);
    }

    private void onSelectFile() {
        final FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        fileDescriptor.withTitle("Select Path for Azurite");
        final VirtualFile file = FileChooser.chooseFile(fileDescriptor, null, null);
        if (file != null && file.exists()) {
            final String path = file.getPath();
            if (!getItems().contains(path)) {
                this.draftPaths.add(path);
            }
            this.reloadItems();
            this.setValue(path);
        }
    }
}
