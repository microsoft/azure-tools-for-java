/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.components.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.ProjectUtils;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.keyvalue.KeyValueResource;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionConnectionComboBox extends AzureComboBox<FunctionConnectionComboBox.ConnectionConfiguration> {
    public static final ConnectionConfiguration EMPTY = new ConnectionConfiguration(StringUtils.EMPTY, StringUtils.EMPTY);
    private static final String LOCAL_SETTINGS_VALUES = "Values";

    private final Project project;
    private final String resourceType;
    private final Module module;
    private VirtualFile localSettingsFile;
    @Getter
    @Setter
    private String propertyName;

    public FunctionConnectionComboBox(final Module module, final String resourceType, final String propertyName) {
        super(false);
        this.module = module;
        this.project = module.getProject();
        this.resourceType = resourceType;
        this.propertyName = propertyName;
        setLocalSettings(getLocalSettingsFromModule(module));
    }

    @Nullable
    private VirtualFile getLocalSettingsFromModule(final Module module) {
        return ReadAction.compute(() -> FilenameIndex.getVirtualFilesByName("local.settings.json", GlobalSearchScope.projectScope(project))).stream()
            .filter(file -> ProjectUtils.isSameModule(ModuleUtil.findModuleForFile(file, project), module))
            .filter(file -> Objects.nonNull(file.getCanonicalPath()))
            .min(Comparator.comparing(VirtualFile::getCanonicalPath))
            .orElse(null);
    }

    public void setLocalSettings(final VirtualFile virtualFile) {
        if (Objects.equals(virtualFile, this.localSettingsFile)) {
            return;
        }
        this.localSettingsFile = virtualFile;
        if (virtualFile == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Nonnull
    @Override
    protected List<? extends ConnectionConfiguration> loadItems() throws Exception {
        return ListUtils.union(getConfigurationFromResourceConnection(), getConfigurationFromLocalSettings());
    }

    private List<ConnectionConfiguration> getConfigurationFromResourceConnection() {
        if (Objects.isNull(module)) {
            return Collections.emptyList();
        }
        final List<Connection<?, ?>> connections = Optional.ofNullable(AzureModule.from(module))
            .map(AzureModule::getDefaultProfile)
            .map(Profile::getConnections)
            .orElse(Collections.emptyList());
        return connections.stream().map(ConnectionConfiguration::new).collect(Collectors.toList());
    }

    private List<ConnectionConfiguration> getConfigurationFromLocalSettings() {
        if (Objects.isNull(localSettingsFile) || !localSettingsFile.exists()) {
            return Collections.emptyList();
        }
        return readAppSettings(localSettingsFile).keySet().stream()
            .map(s -> new ConnectionConfiguration(s, null))
            .collect(Collectors.toList());
    }

    // todo: move to function utils in app service module
    private Map<String, String> readAppSettings(final VirtualFile virtualFile) {
        try (final InputStream inputStream = virtualFile.getInputStream()) {
            final Map<String, Object> map = new ObjectMapper().readValue(inputStream, Map.class);
            if (MapUtils.isEmpty(map)) {
                return new HashMap<>();
            }
            return ((Map<?, ?>) map.get(LOCAL_SETTINGS_VALUES)).entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString()));
        } catch (final IOException e) {
            AzureMessager.getMessager().warning(AzureString.format("Failed to read local.setting.json from %s", virtualFile.getCanonicalPath()));
        }
        return Collections.emptyMap();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof ConnectionConfiguration ? ((ConnectionConfiguration) item).getName() : super.getItemText(item);
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object item) {
        if (item instanceof ConnectionConfiguration) {
            final String icon = ((ConnectionConfiguration) item).getIcon();
            return StringUtils.isEmpty(icon) ? AllIcons.FileTypes.Text : IntelliJAzureIcons.getIcon(icon);
        }
        return super.getItemIcon(item);
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Create new resource connection (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.Add, tooltip, this::createConnection);
        this.registerShortcut(keyStroke, addEx);
        return Collections.singletonList(addEx);
    }

    private void createConnection() {
        final FunctionConnectionCreationDialog dialog = new FunctionConnectionCreationDialog(project, module, resourceType);
        final Action.Id<FunctionConnectionComboBox.ConnectionConfiguration> actionId = Action.Id.of("user/connector.add_function_connection.connection");
        dialog.setOkAction(new Action<>(actionId)
            .withLabel("Add")
            .withIdParam(FunctionConnectionComboBox.ConnectionConfiguration::getName)
            .withAuthRequired(false)
            .withHandler(connection -> {
                FunctionConnectionComboBox.this.reloadItems();
                FunctionConnectionComboBox.this.setValue(connection);
            }));
        dialog.show();
    }

    @Data
    @Builder
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    public static class ConnectionConfiguration {
        @EqualsAndHashCode.Include
        private String name;
        private String icon;

        public ConnectionConfiguration(final Connection<?, ?> connection) {
            // for common connection, use connection name, for app service connection, use env prefix
            this.name = connection.getResource().getDefinition() instanceof KeyValueResource.Definition ?
                connection.getResource().getName() : connection.getEnvPrefix();
            this.icon = connection.getResource().getDefinition().getIcon();
        }
    }
}
