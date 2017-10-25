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

package com.microsoft.intellij.helpers.webapp;

import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azuretools.core.mvp.ui.webapp.WebAppProperty;
import com.microsoft.intellij.helpers.base.BaseEditor;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppPropertyViewPresenter;

public class WebAppPropertyView extends BaseEditor implements WebAppPropertyMvpView {

    public static final String ID = "com.microsoft.intellij.helpers.webapp.WebAppPropertyView";

    private final WebAppPropertyViewPresenter<WebAppPropertyView> presenter;
    private final String sid;
    private final String resId;
    private Map<String, String> cachedAppSettings;
    private Map<String, String> editedAppSettings;

    private static final String PNL_OVERVIEW = "Overview";
    private static final String PNL_APP_SETTING = "App Settings";
    private static final String BUTTON_EDIT = "Edit";
    private static final String BUTTON_REMOVE = "Remove";
    private static final String BUTTON_ADD = "Add";
    private static final String TABLE_HEADER_VALUE = "Value";
    private static final String TABLE_HEADER_KEY = "Key";
    private static final String TXT_NA = "N/A";

    private JPanel pnlMain;
    private JButton btnGetPublishFile;
    private JButton btnSave;
    private JButton btnDiscard;
    private JPanel pnlOverviewHolder;
    private JPanel pnlOverview;
    private JPanel pnlAppSettingsHolder;
    private JPanel pnlAppSettings;
    private JTextField txtResourceGroup;
    private JTextField txtStatus;
    private JTextField txtLocation;
    private JTextField txtSubscription;
    private JTextField txtAppServicePlan;
    private JTextField txtUrl;
    private JTextField txtPricingTier;
    private JTextField txtJavaVersion;
    private JTextField txtContainer;
    private JTextField txtContainerVersion;
    private JLabel lblJavaVersion;
    private JLabel lblContainer;
    private JLabel lblContainerVersion;
    private JBTable tblAppSetting;
    private AnActionButton btnAdd;
    private AnActionButton btnRemove;
    private AnActionButton btnEdit;

    /**
     * Initialize the Web App Property View and return it.
     */
    public static WebAppPropertyView create(@NotNull String sid, @NotNull String resId) {
        WebAppPropertyView view = new WebAppPropertyView(sid, resId);
        view.onLoadWebAppProperty();
        return view;
    }

    private WebAppPropertyView(@NotNull String sid, @NotNull String resId) {
        this.sid = sid;
        this.resId = resId;
        cachedAppSettings = new HashMap<>();
        editedAppSettings = new HashMap<>();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.presenter = new WebAppPropertyViewPresenter<>();
        this.presenter.onAttachView(this);

        // initialize widgets...
        HideableDecorator overviewDecorator = new HideableDecorator(pnlOverviewHolder, PNL_OVERVIEW,
                false /*adjustWindow*/);
        overviewDecorator.setContentComponent(pnlOverview);
        overviewDecorator.setOn(true);

        HideableDecorator appSettingDecorator = new HideableDecorator(pnlAppSettingsHolder, PNL_APP_SETTING,
                false /*adjustWindow*/);
        appSettingDecorator.setContentComponent(pnlAppSettings);
        appSettingDecorator.setOn(true);

        setTextFieldStyle();
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return pnlMain;
    }

    @NotNull
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public void dispose() {
        presenter.onDetachView();
    }

    private void createUIComponents() {
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn(TABLE_HEADER_KEY);
        tableModel.addColumn(TABLE_HEADER_VALUE);

        tblAppSetting = new JBTable(tableModel);
        tblAppSetting.setRowSelectionAllowed(true);
        tblAppSetting.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblAppSetting.addPropertyChangeListener(evt -> {
            if ("tableCellEditor".equals(evt.getPropertyName())) {
                if (!tblAppSetting.isEditing()) {
                    editedAppSettings.clear();
                    DefaultTableModel model = ((DefaultTableModel) tblAppSetting.getModel());
                    int row = 0;
                    while (row < tblAppSetting.getRowCount()) {
                        Object keyObj = model.getValueAt(row, 0);
                        String key = "";
                        String value = "";
                        if (keyObj != null) {
                            key = (String) keyObj;
                        }
                        if (key.isEmpty() || editedAppSettings.containsKey(key)) {
                            model.removeRow(row);
                            continue;
                        }
                        Object valueObj = model.getValueAt(row, 1);
                        if (valueObj != null) {
                            value = (String) valueObj;
                        }
                        editedAppSettings.put(key, value);
                        ++row;
                    }
                    updateSaveAndDiscardBtnStatus();
                    updateTableActionBtnStatus(false);
                } else {
                    updateTableActionBtnStatus(true);
                }
            }
        });

        btnAdd = new AnActionButton(BUTTON_ADD, AllIcons.General.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                DefaultTableModel model = ((DefaultTableModel) tblAppSetting.getModel());
                if (tblAppSetting.isEditing()) {
                    tblAppSetting.getCellEditor().stopCellEditing();
                }
                model.addRow(new String[]{"", ""});
                tblAppSetting.editCellAt(tblAppSetting.getRowCount() - 1, 0);
            }
        };

        btnRemove = new AnActionButton(BUTTON_REMOVE, AllIcons.General.Remove) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                int selectedRow = tblAppSetting.getSelectedRow();
                if (selectedRow == -1) {
                    return;
                }
                DefaultTableModel model = ((DefaultTableModel) tblAppSetting.getModel());
                editedAppSettings.remove(model.getValueAt(selectedRow, 0));
                model.removeRow(selectedRow);
                updateSaveAndDiscardBtnStatus();
            }
        };

        btnEdit = new AnActionButton(BUTTON_EDIT, AllIcons.Actions.Edit) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                int selectedRow = tblAppSetting.getSelectedRow();
                int selectedCol = tblAppSetting.getSelectedColumn();
                if (selectedRow == -1 || selectedCol == -1) {
                    return;
                }
                tblAppSetting.editCellAt(selectedRow, selectedCol);
            }
        };

        ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(tblAppSetting)
                .addExtraActions(btnAdd, btnRemove, btnEdit).setToolbarPosition(ActionToolbarPosition.RIGHT);
        pnlAppSettings = tableToolbarDecorator.createPanel();
    }

    private void updateSaveAndDiscardBtnStatus() {
        if (!editedAppSettings.equals(cachedAppSettings)) {
            btnDiscard.setEnabled(true);
            btnSave.setEnabled(true);
        } else {
            btnDiscard.setEnabled(false);
            btnSave.setEnabled(false);
        }
    }

    private void updateTableActionBtnStatus(boolean isEditing) {
        btnAdd.setEnabled(!isEditing);
        btnRemove.setEnabled(!isEditing);
        btnEdit.setEnabled(!isEditing);
    }

    private void setTextFieldStyle() {
        txtResourceGroup.setBorder(BorderFactory.createEmptyBorder());
        txtStatus.setBorder(BorderFactory.createEmptyBorder());
        txtLocation.setBorder(BorderFactory.createEmptyBorder());
        txtSubscription.setBorder(BorderFactory.createEmptyBorder());
        txtAppServicePlan.setBorder(BorderFactory.createEmptyBorder());
        txtUrl.setBorder(BorderFactory.createEmptyBorder());
        txtPricingTier.setBorder(BorderFactory.createEmptyBorder());
        txtJavaVersion.setBorder(BorderFactory.createEmptyBorder());
        txtContainer.setBorder(BorderFactory.createEmptyBorder());
        txtContainerVersion.setBorder(BorderFactory.createEmptyBorder());

        txtResourceGroup.setBackground(null);
        txtStatus.setBackground(null);
        txtLocation.setBackground(null);
        txtSubscription.setBackground(null);
        txtAppServicePlan.setBackground(null);
        txtUrl.setBackground(null);
        txtPricingTier.setBackground(null);
        txtJavaVersion.setBackground(null);
        txtContainer.setBackground(null);
        txtContainerVersion.setBackground(null);
    }

    private void $$$setupUI$$$() {
    }

    @Override
    public void onLoadWebAppProperty() {
        presenter.onLoadWebAppProperty(this.sid, this.resId);
    }

    @Override
    public void showProperty(WebAppProperty webAppProperty) {
        txtResourceGroup.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_RESOURCE_GRP) == null ? TXT_NA
                : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_RESOURCE_GRP));
        txtStatus.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_STATUS) == null ? TXT_NA
                : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_STATUS));
        txtLocation.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_LOCATION) == null ? TXT_NA
                : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_LOCATION));
        txtSubscription.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_SUB_ID) == null ? TXT_NA
                : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_SUB_ID));
        txtAppServicePlan.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_PLAN) == null ? TXT_NA
                : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_PLAN));
        txtUrl.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_URL) == null ? TXT_NA
                : "http://" + webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_URL));
        txtPricingTier.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_PRICING) == null ? TXT_NA
                : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_PRICING));
        Object os = webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_OPERATING_SYS);
        if (os != null && os instanceof OperatingSystem) {
            switch ((OperatingSystem) os) {
                case WINDOWS:
                    txtJavaVersion.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_JAVA_VERSION) == null
                            ? TXT_NA : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_JAVA_VERSION));
                    txtContainer.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_JAVA_CONTAINER) == null
                            ? TXT_NA : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_JAVA_CONTAINER));
                    txtContainerVersion.setText(webAppProperty
                            .getValue(WebAppPropertyViewPresenter.KEY_JAVA_CONTAINER_VERSION) == null ? TXT_NA
                            : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_JAVA_CONTAINER_VERSION));
                    txtJavaVersion.setVisible(true);
                    txtContainer.setVisible(true);
                    txtContainerVersion.setVisible(true);
                    lblJavaVersion.setVisible(true);
                    lblContainer.setVisible(true);
                    lblContainerVersion.setVisible(true);
                    break;
                case LINUX:
                    txtJavaVersion.setVisible(false);
                    txtContainer.setVisible(false);
                    txtContainerVersion.setVisible(false);
                    lblJavaVersion.setVisible(false);
                    lblContainer.setVisible(false);
                    lblContainerVersion.setVisible(false);
                    break;
                default:
                    break;
            }
        }


        DefaultTableModel model = (DefaultTableModel) tblAppSetting.getModel();
        model.getDataVector().clear();
        cachedAppSettings.clear();
        Object appSettingsObj = webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_APP_SETTING);
        if (appSettingsObj != null && appSettingsObj instanceof Map) {
            Map<String, String> appSettings = (Map<String, String>) appSettingsObj;
            for (String key : appSettings.keySet()) {
                model.addRow(new String[]{key, appSettings.get(key)});
                cachedAppSettings.put(key, appSettings.get(key));
            }
        }
        pnlOverview.revalidate();
    }
}
