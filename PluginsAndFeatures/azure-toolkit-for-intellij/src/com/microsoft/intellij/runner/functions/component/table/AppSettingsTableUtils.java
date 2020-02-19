/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.functions.component.table;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelperFactory;
import com.microsoft.intellij.runner.functions.AzureFunctionsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class AppSettingsTableUtils {

    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{\"IsEncrypted\":false,\"Values\":{\"AzureWebJobsStorage\":\"\",\"FUNCTIONS_WORKER_RUNTIME\":\"java\"}}";
    private static final String LOCAL_SETTINGS_VALUES = "Values";
    private static final String EXPORT_APP_SETTINGS = "Export app settings to local.settings.json";
    private static final String EXPORT_APP_SETTINGS_FAILED = "Fail to save app settings to local.settings.json";
    private static final String LOCAL_SETTINGS_JSON = "local.settings.json";
    private static final String EXPORT_LOCAL_SETTINGS_TITLE = "Export to local settings";

    public static JPanel decorateAppSettingTable(AppSettingsTable appSettingsTable) {
        final JPanel result = new JPanel();
        // create the parent panel which contains app settings table and prompt panel
        result.setLayout(new GridLayoutManager(2, 1));
        final JTextPane promptPanel = new JTextPane();
        final GridConstraints paneConstraint = new GridConstraints(1, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 7, 7, null, null, null);
        result.add(promptPanel, paneConstraint);

        final AnActionButton btnAdd = new AnActionButton("Add", AllIcons.General.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                final String key = JOptionPane.showInputDialog("Please input value for key: ");
                final String value = JOptionPane.showInputDialog("Please input value for key: ");
                appSettingsTable.addAppSettings(key, value);
                appSettingsTable.repaint();
            }
        };

        final AnActionButton btnRemove = new AnActionButton("Remove", AllIcons.General.Remove) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                appSettingsTable.removeAppSettings(appSettingsTable.getSelectedRow());
                appSettingsTable.repaint();
            }
        };

        final AnActionButton importButton = new AnActionButton("Import", AllIcons.General.ImportProject) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                // todo: add import dialog
            }
        };

        final AnActionButton exportButton = new AnActionButton(EXPORT_LOCAL_SETTINGS_TITLE, AllIcons.General.ExportSettings) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                try {
                    final File file = DefaultLoader.getUIHelper().showFileSaver(EXPORT_APP_SETTINGS, LOCAL_SETTINGS_JSON);
                    AppSettingsTableUtils.exportLocalSettingsJsonFile(file, appSettingsTable.getAppSettings());
                } catch (IOException e) {
                    MvpUIHelperFactory.getInstance().getMvpUIHelper().showException(EXPORT_APP_SETTINGS_FAILED, e);
                }
            }
        };

        appSettingsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                final String prompt = AzureFunctionsConstants.getAppSettingHint(appSettingsTable.getSelectedKey());
                promptPanel.setText(prompt);
            }
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
        try {
            final JsonObject jsonObject = getLocalSettingsJsonObject(target);
            final JsonObject valueObject = jsonObject.getAsJsonObject(LOCAL_SETTINGS_VALUES);
            valueObject.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
            return result;
        } catch (JsonParseException e) {
            return new HashMap<>();
        }
    }

    public static void exportLocalSettingsJsonFile(File target, Map<String, String> appSettings) throws IOException {
        if (target == null) {
            return;
        }
        if (!target.exists()) {
            target.createNewFile();
        }
        final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonObject jsonObject = getLocalSettingsJsonObject(target);
        if (jsonObject == null) {
            jsonObject = gson.fromJson(DEFAULT_LOCAL_SETTINGS_JSON, JsonObject.class);
        }
        final JsonObject valueObject = new JsonObject();
        appSettings.entrySet().forEach(entry -> valueObject.addProperty(entry.getKey(), entry.getValue()));
        jsonObject.add(LOCAL_SETTINGS_VALUES, valueObject);
        try (Writer writer = new FileWriter(target)) {
            gson.toJson(jsonObject, writer);
        }
    }

    private static JsonObject getLocalSettingsJsonObject(File target) {
        try (FileInputStream fis = new FileInputStream(target);
             InputStreamReader isr = new InputStreamReader(fis)) {
            final Gson gson = new Gson();
            return gson.fromJson(isr, JsonObject.class);
        } catch (IOException | JsonParseException e) {
            return null;
        }
    }
}
