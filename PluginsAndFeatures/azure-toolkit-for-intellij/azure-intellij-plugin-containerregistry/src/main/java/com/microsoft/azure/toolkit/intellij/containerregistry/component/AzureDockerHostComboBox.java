/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.container.DockerUtil;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class AzureDockerHostComboBox extends AzureComboBox<DockerHost> {
    private static final String DOCKER_HOST_HISTORY = "azure_toolkit.docker.docker_host_history";
    private static final int MAX_HISTORY_SIZE = 15;
    private final Project project;

    public AzureDockerHostComboBox(Project project) {
        super();
        this.project = project;
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof DockerHost ? ((DockerHost) item).getDockerHost() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends DockerHost> loadItems() throws Exception {
        return Stream.of(DockerUtil.getDockerHosts(), loadHistory()).flatMap(List::stream).distinct().toList();
    }

    // todo: make it an configuration item in AzureConfiguration
    private List<DockerHost> loadHistory() {
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        final String history = propertiesComponent.getValue(DOCKER_HOST_HISTORY);
        if (StringUtils.isNotEmpty(history)) {
            try {
                return JsonUtils.fromJson(history, new TypeReference<>() {
                });
            } catch (final Exception ignore) {
                // ignore since the history data is not important
            }
        }
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final List<ExtendableTextComponent.Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("%s (%s)", "Create New Docker Host Configuration", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.Add, tooltip, this::createDockerHostConfiguration);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void createDockerHostConfiguration() {
        final DockerHostCreationDialog dialog = new DockerHostCreationDialog(project);
        dialog.setOkActionListener((dockerHost) -> {
            dialog.close();
            this.addItem(dockerHost);
            this.setValue(dockerHost);
            saveHistory();
        });
        dialog.show();
    }

    private void saveHistory() {
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        final List<DockerHost> items = getItems();
        final List<DockerHost> itemsToSave = items.subList(items.size() < MAX_HISTORY_SIZE ? 0 : items.size() - MAX_HISTORY_SIZE, items.size());
        propertiesComponent.setValue(DOCKER_HOST_HISTORY, JsonUtils.toJson(itemsToSave));
    }
}
