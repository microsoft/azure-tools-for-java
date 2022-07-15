/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table;

import com.google.gson.JsonObject;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.ide.appservice.util.JsonUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.AzureFunctionsConstants;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class AppSettingsTableUtils {

    private static final String DEFAULT_LOCAL_SETTINGS_JSON =
            "{\"IsEncrypted\":false,\"Values\":{\"AzureWebJobsStorage\":\"\",\"FUNCTIONS_WORKER_RUNTIME\":\"java\"}}";
    private static final String LOCAL_SETTINGS_VALUES = "Values";
    private static final String LOCAL_SETTINGS_JSON = "local.settings.json";

    public static JPanel createAppSettingPanel(AppSettingsTable appSettingsTable) {
        final JPanel result = new JPanel();
        // create the parent panel which contains app settings table and prompt panel
        result.setLayout(new GridLayoutManager(2, 1));
        final JTextPane promptPanel = new JTextPane();
        final GridConstraints paneConstraint = new GridConstraints(1, 0, 1, 1, 0,
                GridConstraints.FILL_BOTH, 7, 7, null, null, null);
        promptPanel.setFocusable(false);
        result.add(promptPanel, paneConstraint);

        final AnActionButton btnAdd = new AnActionButton(message("common.add"), AllIcons.General.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                final String key = Messages.showInputDialog(appSettingsTable, message("function.appSettings.add.key.message"),
                        message("function.appSettings.add.key.title"), null);
                if (StringUtils.isEmpty(key)) {
                    return;
                }
                final String value = Messages.showInputDialog(appSettingsTable, message("function.appSettings.add.value.message"),
                        message("function.appSettings.add.value.title"), null);
                appSettingsTable.addAppSettings(key, value);
                appSettingsTable.repaint();
            }
        };
        btnAdd.registerCustomShortcutSet(KeyEvent.VK_ADD, InputEvent.ALT_DOWN_MASK, result);

        final AnActionButton btnRemove = new AnActionButton(message("common.remove"), AllIcons.General.Remove) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                try {
                    appSettingsTable.removeAppSettings(appSettingsTable.getSelectedRow());
                    appSettingsTable.repaint();
                } catch (final IllegalArgumentException iae) {
                    AzureMessager.getMessager().error(message("function.appSettings.remove.error.title"), iae.getMessage());
                }
            }
        };
        btnRemove.registerCustomShortcutSet(KeyEvent.VK_SUBTRACT, InputEvent.ALT_DOWN_MASK, result);

        final AnActionButton importButton = new AnActionButton(message("common.import"), AllIcons.ToolbarDecorator.Import) {
            @Override
            @AzureOperation(name = "function.import_app_settings", type = AzureOperation.Type.TASK)
            public void actionPerformed(AnActionEvent anActionEvent) {
                final ImportAppSettingsDialog importAppSettingsDialog = new ImportAppSettingsDialog(appSettingsTable.getLocalSettingsPath());
                importAppSettingsDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent windowEvent) {
                        super.windowClosed(windowEvent);
                        final Map<String, String> appSettings = importAppSettingsDialog.getAppSettings();
                        if (importAppSettingsDialog.shouldErase()) {
                            appSettingsTable.clear();
                        }
                        if (appSettings != null) {
                            appSettingsTable.addAppSettings(appSettings);
                        }
                    }
                });
                importAppSettingsDialog.setLocationRelativeTo(appSettingsTable);
                importAppSettingsDialog.pack();
                importAppSettingsDialog.setVisible(true);
            }
        };
        importButton.registerCustomShortcutSet(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK, result);

        final AnActionButton exportButton = new AnActionButton(message("function.appSettings.export.title"), AllIcons.ToolbarDecorator.Export) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                try {
                    final FileSaverDescriptor fileDescriptor = new FileSaverDescriptor(message("function.appSettings.export.description"), "");
                    final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileDescriptor, (Project) null);
                    final VirtualFile userHome = LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home"));
                    final VirtualFileWrapper fileWrapper = dialog.save(userHome, LOCAL_SETTINGS_JSON);
                    final File file = Optional.ofNullable(fileWrapper).map(VirtualFileWrapper::getFile).orElse(null);
                    if (file != null) {
                        AppSettingsTableUtils.exportLocalSettingsJsonFile(file, appSettingsTable.getAppSettings());
                        AzureMessager.getMessager().info(message("function.appSettings.export.succeed.title"), message("function.appSettings.export.succeed.message"));
                    }
                } catch (final IOException e) {
                    final String title = message("function.appSettings.export.error.title");
                    final String message = message("function.appSettings.export.error.failedToSave", e.getMessage());
                    AzureMessager.getMessager().error(title, message);
                }
            }
        };
        exportButton.registerCustomShortcutSet(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK, result);

        appSettingsTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            final String prompt = AzureFunctionsConstants.getAppSettingHint(appSettingsTable.getSelectedKey());
            promptPanel.setText(prompt);
        });

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

        final ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(appSettingsTable)
                .addExtraActions(btnAdd, btnRemove, importButton, exportButton).setToolbarPosition(ActionToolbarPosition.RIGHT);
        final JPanel tablePanel = tableToolbarDecorator.createPanel();
        final GridConstraints tableConstraint = new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 7, 7, null, null, null);
        result.add(tablePanel, tableConstraint);
        result.setMinimumSize(new Dimension(-1, 100));
        return result;
    }

    public static Map<String, String> getAppSettingsFromLocalSettingsJson(File target) {
        final Map<String, String> result = new HashMap<>();
        final JsonObject jsonObject = JsonUtils.readJsonFile(target);
        if (jsonObject == null) {
            return new HashMap<>();
        }
        final JsonObject valueObject = jsonObject.getAsJsonObject(LOCAL_SETTINGS_VALUES);
        valueObject.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
        return result;
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
        JsonObject jsonObject = JsonUtils.readJsonFile(target);
        if (jsonObject == null) {
            jsonObject = JsonUtils.fromJsonString(DEFAULT_LOCAL_SETTINGS_JSON, JsonObject.class);
        }
        final JsonObject valueObject = new JsonObject();
        appSettings.forEach(valueObject::addProperty);
        jsonObject.add(LOCAL_SETTINGS_VALUES, valueObject);
        JsonUtils.writeJsonToFile(target, jsonObject);
    }

}
