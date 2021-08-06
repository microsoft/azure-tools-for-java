/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component.table;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.ToolbarDecorator;
import com.microsoft.azure.toolkit.intellij.function.runner.AzureFunctionsConstants;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.intellij.CommonConst;
import com.microsoft.intellij.helpers.UIHelperImpl;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ImportAppSettingsDialog extends JDialog implements ImportAppSettingsView {
    private static final String LOCAL_SETTINGS_JSON = "local.settings.json";
    private JPanel contentPanel;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox cbAppSettingsSource;
    private AppSettingsTable tblAppSettings;
    private JLabel lblAppSettingsSource;
    private JPanel pnlAppSettings;
    private JCheckBox chkErase;

    private boolean eraseExistingSettings;
    private Map<String, String> appSettings = null;
    private final AppSettingsDialogPresenter<ImportAppSettingsDialog> presenter = new AppSettingsDialogPresenter<>();

    public ImportAppSettingsDialog(Path localSettingsPath) {
        super();
        setContentPane(contentPanel);
        setModal(true);
        setTitle(message("function.appSettings.import.title"));
        setMinimumSize(new Dimension(-1, 250));
        setAlwaysOnTop(true);
        getRootPane().setDefaultButton(buttonOK);

        this.presenter.onAttachView(this);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        cbAppSettingsSource.setRenderer(new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object object, int index, boolean isSelected, boolean cellHasFocus) {
                if (object instanceof IFunctionApp) {
                    setIcon(UIHelperImpl.loadIcon(AzureFunctionsConstants.AZURE_FUNCTIONS_ICON));
                    setText(((IFunctionApp) object).name());
                } else if (LOCAL_SETTINGS_JSON.equals(object)) {
                    setText(object.toString());
                    setIcon(AllIcons.FileTypes.Json);
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        cbAppSettingsSource.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                final Object selectedItem = cbAppSettingsSource.getSelectedItem();
                if (selectedItem instanceof IFunctionApp) {
                    presenter.onLoadFunctionAppSettings((IFunctionApp) selectedItem);
                } else if (LOCAL_SETTINGS_JSON.equals(selectedItem)) {
                    presenter.onLoadLocalSettings(localSettingsPath);
                }
            }
        });

        chkErase.addActionListener(e -> eraseExistingSettings = chkErase.isSelected());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPanel.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        cbAppSettingsSource.addItem(LOCAL_SETTINGS_JSON);
        presenter.onLoadLocalSettings(localSettingsPath);

        presenter.onLoadFunctionApps();
        pack();
    }

    @Override
    public void fillFunctionApps(List<IFunctionApp> functionApps) {
        functionApps.forEach(functionAppResourceEx -> cbAppSettingsSource.addItem(functionAppResourceEx));
        pack();
    }

    @Override
    public void fillFunctionAppSettings(Map<String, String> appSettings) {
        tblAppSettings.setAppSettings(appSettings);
        if (appSettings.size() == 0) {
            tblAppSettings.getEmptyText().setText(CommonConst.EMPTY_TEXT);
        }
    }

    @Override
    public void beforeFillAppSettings() {
        tblAppSettings.getEmptyText().setText(CommonConst.LOADING_TEXT);
        tblAppSettings.clear();
    }

    public boolean shouldErase() {
        return eraseExistingSettings;
    }

    public Map<String, String> getAppSettings() {
        return this.appSettings;
    }

    private void createUIComponents() {
        tblAppSettings = new AppSettingsTable("");
        tblAppSettings.getEmptyText().setText(CommonConst.LOADING_TEXT);
        pnlAppSettings = ToolbarDecorator.createDecorator(tblAppSettings).createPanel();
    }

    private void onOK() {
        this.appSettings = tblAppSettings.getAppSettings();
        dispose();
    }

    private void onCancel() {
        this.appSettings = null;
        this.eraseExistingSettings = false;
        dispose();
    }
}
