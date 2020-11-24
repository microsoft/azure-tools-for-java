/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.functions.component.table;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.ToolbarDecorator;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.intellij.common.CommonConst;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.runner.functions.AzureFunctionsConstants;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    private Path localSettingsPath;
    private boolean eraseExistingSettings;
    private Map<String, String> appSettings = null;
    private AppSettingsDialogPresenter<ImportAppSettingsDialog> presenter = new AppSettingsDialogPresenter<>();

    public ImportAppSettingsDialog(Path localSettingsPath) {
        setContentPane(contentPanel);
        setModal(true);
        setTitle(message("function.appSettings.import.title"));
        setMinimumSize(new Dimension(-1, 250));
        setAlwaysOnTop(true);
        getRootPane().setDefaultButton(buttonOK);

        this.localSettingsPath = localSettingsPath;
        this.presenter.onAttachView(this);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        cbAppSettingsSource.setRenderer(new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object object, int index, boolean isSelected, boolean cellHasFocus) {
                if (object instanceof ResourceEx) {
                    setIcon(UIHelperImpl.loadIcon(AzureFunctionsConstants.AZURE_FUNCTIONS_ICON));
                    setText(((ResourceEx<FunctionApp>) object).getResource().name());
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
                if (selectedItem instanceof ResourceEx) {
                    final ResourceEx<FunctionApp> app = ((ResourceEx<FunctionApp>) selectedItem);
                    presenter.onLoadFunctionAppSettings(app.getSubscriptionId(), app.getResource().id());
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

        contentPanel.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        cbAppSettingsSource.addItem(LOCAL_SETTINGS_JSON);
        presenter.onLoadLocalSettings(localSettingsPath);

        presenter.onLoadFunctionApps();
        pack();
    }

    @Override
    public void fillFunctionApps(List<ResourceEx<FunctionApp>> functionApps) {
        functionApps.stream().forEach(functionAppResourceEx -> cbAppSettingsSource.addItem(functionAppResourceEx));
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
