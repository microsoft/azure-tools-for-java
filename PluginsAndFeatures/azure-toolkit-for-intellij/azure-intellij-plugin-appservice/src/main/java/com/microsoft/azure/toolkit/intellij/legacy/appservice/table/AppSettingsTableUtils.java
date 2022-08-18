/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.table;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.nimbusds.jose.util.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class AppSettingsTableUtils {
    public static JPanel createAppSettingPanel(AppSettingsTable appSettingsTable, AnActionButton... additionalActions) {
        final AnActionButton btnAdd = new AnActionButton(message("common.add"), AllIcons.General.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                addNewProperty(appSettingsTable);
            }
        };
        btnAdd.registerCustomShortcutSet(KeyEvent.VK_ADD, InputEvent.ALT_DOWN_MASK, appSettingsTable);

        final AnActionButton btnRemove = new AnActionButton(message("common.remove"), AllIcons.General.Remove) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                removeProperty(appSettingsTable);
            }
        };
        btnRemove.registerCustomShortcutSet(KeyEvent.VK_SUBTRACT, InputEvent.ALT_DOWN_MASK, appSettingsTable);
        final AnActionButton[] actionButtons = {btnAdd, btnRemove};
        final ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(appSettingsTable)
                .addExtraActions(ArrayUtils.concat(actionButtons, additionalActions)).setToolbarPosition(ActionToolbarPosition.RIGHT);
        return tableToolbarDecorator.createPanel();
    }

    private static void removeProperty(@Nonnull final AppSettingsTable appSettingsTable) {
        try {
            appSettingsTable.removeAppSettings(appSettingsTable.getSelectedRow());
            appSettingsTable.repaint();
        } catch (final IllegalArgumentException iae) {
            AzureMessager.getMessager().error(message("function.appSettings.remove.error.title"), iae.getMessage());
        }
    }

    private static void addNewProperty(@Nonnull final AppSettingsTable appSettingsTable) {
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
}
