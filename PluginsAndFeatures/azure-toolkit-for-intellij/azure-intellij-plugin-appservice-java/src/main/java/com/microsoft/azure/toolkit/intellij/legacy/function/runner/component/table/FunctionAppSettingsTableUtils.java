/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.AnActionButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTableUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.AzureFunctionsConstants;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class FunctionAppSettingsTableUtils {

    private static final String DEFAULT_LOCAL_SETTINGS_JSON =
            "{\"IsEncrypted\":false,\"Values\":{\"AzureWebJobsStorage\":\"\",\"FUNCTIONS_WORKER_RUNTIME\":\"java\"}}";
    private static final String LOCAL_SETTINGS_VALUES = "Values";
    private static final String LOCAL_SETTINGS_JSON = "local.settings.json";

    public static JPanel createAppSettingPanel(FunctionAppSettingsTable appSettingsTable) {
        final JPanel result = new JPanel();
        // create the parent panel which contains app settings table and prompt panel
        result.setLayout(new GridLayoutManager(2, 1));
        final JTextPane promptPanel = new JTextPane();
        final GridConstraints paneConstraint = new GridConstraints(1, 0, 1, 1, 0,
                GridConstraints.FILL_BOTH, 7, 7, null, null, null);
        promptPanel.setFocusable(false);
        result.add(promptPanel, paneConstraint);

        final AnActionButton importButton = new AnActionButton(message("common.import"), AllIcons.ToolbarDecorator.Import) {
            @Override
            @AzureOperation(name = "user/function.import_app_settings")
            public void actionPerformed(AnActionEvent anActionEvent) {
                importAppSettings(appSettingsTable);
            }
        };
        importButton.registerCustomShortcutSet(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK, result);

        final AnActionButton exportButton = new AnActionButton(message("function.appSettings.export.title"), AllIcons.ToolbarDecorator.Export) {
            @Override
            @AzureOperation(name = "user/function.export_app_settings")
            public void actionPerformed(AnActionEvent anActionEvent) {
                exportAppSettings(appSettingsTable);
            }
        };
        exportButton.registerCustomShortcutSet(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK, result);

        appSettingsTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            final int selectedRow = appSettingsTable.getSelectedRow();
            final String prompt = AzureFunctionsConstants.getAppSettingHint(appSettingsTable.getSelectedKey());
            promptPanel.setText(prompt);
            IntStream.range(0, appSettingsTable.getColumnCount()).forEach(col -> Optional.ofNullable(appSettingsTable.getAccessibleContext())
                    .map(AccessibleContext::getAccessibleTable).map(table -> table.getAccessibleAt(selectedRow, col))
                    .map(Accessible::getAccessibleContext)
                    .ifPresent(context -> context.setAccessibleDescription(prompt)));
        });

        // todo: extract codes for app settings prompt panel
        appSettingsTable.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                final String prompt = AzureFunctionsConstants.getAppSettingHint(appSettingsTable.getSelectedKey());
                promptPanel.setText(prompt);
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                promptPanel.setText("");
            }
        });

        final JPanel tablePanel = AppSettingsTableUtils.createAppSettingPanel(appSettingsTable, importButton, exportButton);
        final GridConstraints tableConstraint = new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 7, 7, null, null, null);
        result.add(tablePanel, tableConstraint);
        return result;
    }

    public static void importAppSettings(@Nonnull final FunctionAppSettingsTable appSettingsTable) {
        final ImportAppSettingsDialog dialog = new ImportAppSettingsDialog(appSettingsTable.getProject());
        dialog.setOkAction(new Action<ImportAppSettingsDialog.Result>(Action.Id.of("user/function.do_import_app_settings"))
                .withLabel("Import")
                .withAuthRequired(false)
                .withHandler((result, event) -> {
                    if (result.isEraseExistingSettings()) {
                        appSettingsTable.clear();
                    }
                    if (result.getAppSettings() != null) {
                        appSettingsTable.addAppSettings(result.getAppSettings());
                    }
                }));
        dialog.pack();
        dialog.show();
    }

    private static void exportAppSettings(@Nonnull final FunctionAppSettingsTable appSettingsTable) {
        try {
            final FileSaverDescriptor fileDescriptor = new FileSaverDescriptor(message("function.appSettings.export.description"), "");
            final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileDescriptor, (Project) null);
            final VirtualFile userHome = LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home"));
            final VirtualFileWrapper fileWrapper = dialog.save(userHome, LOCAL_SETTINGS_JSON);
            final File file = Optional.ofNullable(fileWrapper).map(VirtualFileWrapper::getFile).orElse(null);
            if (file != null) {
                FunctionAppSettingsTableUtils.exportLocalSettingsJsonFile(file, appSettingsTable.getAppSettings());
                AzureMessager.getMessager().info(message("function.appSettings.export.succeed.title"), message("function.appSettings.export.succeed.message"));
            }
        } catch (final IOException e) {
            final String title = message("function.appSettings.export.error.title");
            final String message = message("function.appSettings.export.error.failedToSave", e.getMessage());
            AzureMessager.getMessager().error(title, message);
        }
    }

    public static Map<String, String> getAppSettingsFromLocalSettingsJson(File target) {
        final Map<String, Object> jsonObject = JsonUtils.readFromJsonFile(target, Map.class);
        if (jsonObject == null) {
            return new HashMap<>();
        }
        return ((Map<?, ?>) jsonObject.get(LOCAL_SETTINGS_VALUES)).entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString()));
    }

    public static void exportLocalSettingsJsonFile(File target, Map<String, String> appSettings) throws IOException {
        if (target == null) {
            return;
        }
        final File parentFolder = target.getParentFile();
        if (!parentFolder.exists()) {
            parentFolder.mkdirs();
        }
        if (!target.exists()) {
            target.createNewFile();
        }
        Map<String, Object> jsonObject = JsonUtils.readFromJsonFile(target, Map.class);
        if (jsonObject == null) {
            jsonObject = JsonUtils.fromJson(DEFAULT_LOCAL_SETTINGS_JSON, Map.class);
        }
        final Map<String, Object> valueObject = new HashMap<>(appSettings);
        jsonObject.put(LOCAL_SETTINGS_VALUES, valueObject);
        JsonUtils.writeToJsonFile(target, jsonObject);
    }

}
