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

    /**
     * Initialize the Web App Property View and return it.
     */
    public static WebAppPropertyView initialize(@NotNull String sid, @NotNull String resId) {
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

        AnActionButton btnAdd = new AnActionButton(BUTTON_ADD, AllIcons.General.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {

            }
        };

        AnActionButton btnRemove = new AnActionButton(BUTTON_REMOVE, AllIcons.General.Remove) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {

            }
        };

        AnActionButton btnEdit = new AnActionButton(BUTTON_EDIT, AllIcons.Actions.Edit) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {

            }
        };

        ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(tblAppSetting)
                .addExtraActions(btnAdd, btnRemove, btnEdit).setToolbarPosition(ActionToolbarPosition.RIGHT);
        pnlAppSettings = tableToolbarDecorator.createPanel();
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

    private void $$$setupUI$$$() { }

    @Override
    public void onLoadWebAppProperty() {
        presenter.onLoadWebAppProperty(this.sid, this.resId);
    }

    @Override
    public void showProperty(WebAppProperty webAppProperty) {
        txtResourceGroup.setText(webAppProperty.getGroupName());
        txtStatus.setText(webAppProperty.getStatus());
        txtLocation.setText(webAppProperty.getRegionName());
        txtSubscription.setText(webAppProperty.getSubscriptionId());
        txtAppServicePlan.setText(webAppProperty.getAppServicePlan());
        txtUrl.setText("http://" + webAppProperty.getUrl());
        txtPricingTier.setText(webAppProperty.getPricingTier());
        switch (webAppProperty.getOperatingSystem()) {
            case WINDOWS:
                txtJavaVersion.setText(webAppProperty.getJavaVersion());
                txtContainer.setText(webAppProperty.getContainer());
                txtContainerVersion.setText(webAppProperty.getContainerVersion());
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

        DefaultTableModel model = (DefaultTableModel) tblAppSetting.getModel();
        model.getDataVector().clear();
        cachedAppSettings.clear();
        Map<String, String> appSettings = webAppProperty.getAppSettings();
        for (String key : appSettings.keySet()) {
            model.addRow(new String[]{key, appSettings.get(key)});
            cachedAppSettings.put(key, appSettings.get(key));
        }
        pnlOverview.revalidate();
    }
}
