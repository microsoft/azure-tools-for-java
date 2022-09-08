/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.mvp.ui.webapp.WebAppProperty;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.ui.components.AzureActionListenerWrapper;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppPropertyViewPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class WebAppBasePropertyView extends BaseEditor implements WebAppBasePropertyMvpView {
    protected final String id;
    protected boolean loading = false;
    protected final WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView> presenter;
    private final Map<String, String> cachedAppSettings;
    private final Map<String, String> editedAppSettings;

    private static final String PNL_OVERVIEW = "Overview";
    private static final String PNL_APP_SETTING = "App Settings";
    private static final String BUTTON_EDIT = "Edit";
    private static final String BUTTON_REMOVE = "Remove";
    private static final String BUTTON_ADD = "Add";
    private static final String TABLE_HEADER_VALUE = "Value";
    private static final String TABLE_HEADER_KEY = "Key";
    private static final String TXT_NA = "N/A";
    private static final String TABLE_LOADING_MESSAGE = "Loading ... ";
    private static final String TABLE_EMPTY_MESSAGE = "No available settings.";
    private static final String FILE_SELECTOR_TITLE = "Choose Where You Want to Save the Publish Profile.";
    private static final String NOTIFY_PROPERTY_UPDATE_SUCCESS = "Properties updated.";
    private static final String NOTIFY_PROFILE_GET_SUCCESS = "Publish Profile saved.";
    private static final String NOTIFY_PROFILE_GET_FAIL = "Failed to get Publish Profile.";
    private static final String INSIGHT_NAME = "AzurePlugin.IntelliJ.Editor.WebAppBasePropertyView";

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
    private HyperlinkLabel lnkUrl;
    private JTextField txtPricingTier;
    private JTextField txtJavaVersion;
    private JTextField txtContainer;
    private JLabel lblJavaVersion;
    private JLabel lblContainer;
    private JBTable tblAppSetting;
    private DefaultTableModel tableModel;
    private AnActionButton btnAdd;
    private AnActionButton btnRemove;
    private AnActionButton btnEdit;

    protected String subscriptionId;
    protected String resourceId;
    protected String appServiceId;
    protected String slotName;
    protected Project project;
    protected VirtualFile virtualFile;
    private final AzureEventBus.EventListener listener;

    protected WebAppBasePropertyView(@Nonnull Project project, @Nonnull String sid,
                                     @Nonnull String appServiceId, @Nullable String slotName, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile);
        this.id = getId();
        this.subscriptionId = sid;
        this.appServiceId = appServiceId;
        // workaround to get the resource id of deployment slot
        // todo: @hanli Refactor properties interface to use AppServiceResource directly
        this.resourceId = StringUtils.isEmpty(slotName) ? appServiceId : String.format("%s/slots/%s", appServiceId, slotName);
        this.slotName = slotName;
        this.virtualFile = virtualFile;
        this.project = project;
        this.presenter = createPresenter();
        this.presenter.onAttachView(this);

        cachedAppSettings = new LinkedHashMap<>();
        editedAppSettings = new LinkedHashMap<>();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.

        // initialize widgets...
        final HideableDecorator overviewDecorator = new HideableDecorator(pnlOverviewHolder, PNL_OVERVIEW,
                false /*adjustWindow*/);
        overviewDecorator.setContentComponent(pnlOverview);
        overviewDecorator.setOn(true);

        final HideableDecorator appSettingDecorator = new HideableDecorator(pnlAppSettingsHolder, PNL_APP_SETTING,
                false /*adjustWindow*/);
        appSettingDecorator.setContentComponent(pnlAppSettings);
        appSettingDecorator.setOn(true);

        btnGetPublishFile.addActionListener(new AzureActionListenerWrapper(INSIGHT_NAME, "btnGetPublishFile", null) {
            @Override
            public void actionPerformedFunc(ActionEvent event) {
                EventUtil.executeWithLog(TelemetryConstants.APP_SERVICE, TelemetryConstants.GET_PUBLISH_FILE, operation -> {
                    final FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(
                        false /*chooseFiles*/,
                        true /*chooseFolders*/,
                        false /*chooseJars*/,
                        false /*chooseJarsAsFiles*/,
                        false /*chooseJarContents*/,
                        false /*chooseMultiple*/
                    );
                    fileChooserDescriptor.setTitle(FILE_SELECTOR_TITLE);
                    final VirtualFile file = FileChooser.chooseFile(fileChooserDescriptor, null, null);
                    if (file != null) {
                        presenter.onGetPublishingProfileXmlWithSecrets(sid, appServiceId, slotName, file.getPath());
                    }
                });
            }
        });

        btnDiscard.addActionListener(new AzureActionListenerWrapper(INSIGHT_NAME, "btnDiscard", null) {
            @Override
            public void actionPerformedFunc(ActionEvent event) {
                updateMapStatus(editedAppSettings, cachedAppSettings);
                tableModel.getDataVector().removeAllElements();
                for (final String key : editedAppSettings.keySet()) {
                    tableModel.addRow(new String[]{key, editedAppSettings.get(key)});
                }
                tableModel.fireTableDataChanged();
            }
        });

        btnSave.addActionListener(new AzureActionListenerWrapper(INSIGHT_NAME, "btnSave", null) {
            @Override
            public void actionPerformedFunc(ActionEvent event) {
                EventUtil.executeWithLog(TelemetryConstants.APP_SERVICE, TelemetryConstants.SAVE_APP_SERVICE, operation -> {
                    setLoading(true);
                    presenter.onUpdateWebAppProperty(sid, appServiceId, slotName, cachedAppSettings, editedAppSettings);
                });
            }
        });

        lnkUrl.setHyperlinkText("<Loading...>");
        setTextFieldStyle();

        // todo: add event handler to close editor
        listener = new AzureEventBus.EventListener(this::onStatusChangeEvent);
        AzureEventBus.on("resource.status_changed.resource", listener);
    }

    protected void onStatusChangeEvent(AzureEvent event) {
        final Object source = event.getSource();
        if (source instanceof AppServiceAppBase && StringUtils.equalsIgnoreCase(this.resourceId, ((AppServiceAppBase<?, ?, ?>) source).getId())) {
            final AppServiceAppBase<?, ?, ?> app = (AppServiceAppBase<?, ?, ?>) source;
            if (app.getFormalStatus().isDeleted()) {
                closeEditor(app);
                return;
            }
            if (!loading) {
                presenter.onLoadWebAppProperty(app);
            }
        }
    }

    protected void closeEditor(AppServiceAppBase<?, ?, ?> app) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        AzureTaskManager.getInstance().runLater(() -> fileEditorManager.closeFile(virtualFile));
        final String message = String.format("Close editor of app '%s', because the app is deleted.", app.getName());
        AzureMessager.getMessager().info(message);
    }

    protected abstract String getId();

    protected abstract WebAppBasePropertyViewPresenter createPresenter();

    @Override
    public void onLoadWebAppProperty(@Nonnull final String sid, @Nonnull final String appId,
                                     @Nullable final String slotName) {
        this.presenter.onLoadWebAppProperty(sid, appId, slotName);
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return pnlMain;
    }

    @Nonnull
    @Override
    public String getName() {
        return id;
    }

    @Override
    public void dispose() {
        AzureEventBus.off("resource.status_changed.resource", listener);
        presenter.onDetachView();
    }

    private void createUIComponents() {
        tableModel = new DefaultTableModel();
        tableModel.addColumn(TABLE_HEADER_KEY);
        tableModel.addColumn(TABLE_HEADER_VALUE);

        tblAppSetting = new JBTable(tableModel);
        tblAppSetting.setRowSelectionAllowed(true);
        tblAppSetting.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAppSetting.getEmptyText().setText(TABLE_LOADING_MESSAGE);

        tblAppSetting.addPropertyChangeListener(evt -> {
            if ("tableCellEditor".equals(evt.getPropertyName())) {
                if (!tblAppSetting.isEditing()) {
                    editedAppSettings.clear();
                    int row = 0;
                    while (row < tableModel.getRowCount()) {
                        final Object keyObj = tableModel.getValueAt(row, 0);
                        String key = "";
                        String value = "";
                        if (keyObj != null) {
                            key = (String) keyObj;
                        }
                        if (key.isEmpty() || editedAppSettings.containsKey(key)) {
                            tableModel.removeRow(row);
                            continue;
                        }
                        final Object valueObj = tableModel.getValueAt(row, 1);
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
                if (tblAppSetting.isEditing()) {
                    tblAppSetting.getCellEditor().stopCellEditing();
                }
                tableModel.addRow(new String[]{"", ""});
                tblAppSetting.editCellAt(tblAppSetting.getRowCount() - 1, 0);
            }
        };

        btnRemove = new AnActionButton(BUTTON_REMOVE, AllIcons.General.Remove) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                final int selectedRow = tblAppSetting.getSelectedRow();
                if (selectedRow == -1) {
                    return;
                }
                editedAppSettings.remove(tableModel.getValueAt(selectedRow, 0));
                tableModel.removeRow(selectedRow);
                updateSaveAndDiscardBtnStatus();
            }
        };

        btnEdit = new AnActionButton(BUTTON_EDIT, AllIcons.Actions.Edit) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                final int selectedRow = tblAppSetting.getSelectedRow();
                final int selectedCol = tblAppSetting.getSelectedColumn();
                if (selectedRow == -1 || selectedCol == -1) {
                    return;
                }
                tblAppSetting.editCellAt(selectedRow, selectedCol);
            }
        };

        final ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(tblAppSetting)
                .addExtraActions(btnAdd, btnRemove, btnEdit).setToolbarPosition(ActionToolbarPosition.RIGHT);
        pnlAppSettings = tableToolbarDecorator.createPanel();
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
        final Object url = webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_URL);
        if (url == null) {
            lnkUrl.setHyperlinkText(TXT_NA);
        } else {
            lnkUrl.setHyperlinkText("https://" + url);
            lnkUrl.setHyperlinkTarget("https://" + url);
        }
        txtPricingTier.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_PRICING) == null ? TXT_NA
                : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_PRICING));
        final Object os = webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_OPERATING_SYS);
        if (os instanceof OperatingSystem) {
            switch ((OperatingSystem) os) {
                case WINDOWS:
                case LINUX:
                    txtJavaVersion.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_JAVA_VERSION) == null
                            ? TXT_NA : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_JAVA_VERSION));
                    txtContainer.setText(webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_JAVA_CONTAINER) == null
                            ? TXT_NA : (String) webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_JAVA_CONTAINER));
                    txtJavaVersion.setVisible(true);
                    txtContainer.setVisible(true);
                    lblJavaVersion.setVisible(true);
                    lblContainer.setVisible(true);
                    break;
                case DOCKER:
                    txtJavaVersion.setVisible(false);
                    txtContainer.setVisible(false);
                    lblJavaVersion.setVisible(false);
                    lblContainer.setVisible(false);
                    break;
                default:
                    break;
            }
        }

        tableModel.getDataVector().removeAllElements();
        cachedAppSettings.clear();
        tblAppSetting.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
        final Object appSettingsObj = webAppProperty.getValue(WebAppPropertyViewPresenter.KEY_APP_SETTING);
        if (appSettingsObj instanceof Map) {
            final Map<String, String> appSettings = (Map<String, String>) appSettingsObj;
            for (final String key : appSettings.keySet()) {
                tableModel.addRow(new String[]{key, appSettings.get(key)});
                cachedAppSettings.put(key, appSettings.get(key));
            }
        }
        updateMapStatus(editedAppSettings, cachedAppSettings);
        pnlOverview.revalidate();
        pnlAppSettings.revalidate();
    }

    @Override
    public void showPropertyUpdateResult(boolean isSuccess) {
        setLoading(false);
        if (isSuccess) {
            updateMapStatus(cachedAppSettings, editedAppSettings);
            AzureMessager.getMessager().success(NOTIFY_PROPERTY_UPDATE_SUCCESS);
        }
    }

    @Override
    public void showGetPublishingProfileResult(boolean isSuccess) {
        if (isSuccess) {
            AzureMessager.getMessager().success(NOTIFY_PROFILE_GET_SUCCESS);
        } else {
            AzureMessager.getMessager().error(NOTIFY_PROFILE_GET_FAIL);
        }
    }

    private void updateMapStatus(Map<String, String> to, Map<String, String> from) {
        to.clear();
        to.putAll(from);
        updateSaveAndDiscardBtnStatus();
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        btnSave.setEnabled(!loading);
        btnDiscard.setEnabled(!loading);
        btnAdd.setEnabled(!loading);
        btnRemove.setEnabled(!loading);
        btnEdit.setEnabled(!loading);
        tblAppSetting.setEnabled(!loading);
    }

    private void updateSaveAndDiscardBtnStatus() {
        if (Comparing.equal(editedAppSettings, cachedAppSettings)) {
            btnDiscard.setEnabled(false);
            btnSave.setEnabled(false);
        } else {
            btnDiscard.setEnabled(true);
            btnSave.setEnabled(true);
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
        txtPricingTier.setBorder(BorderFactory.createEmptyBorder());
        txtJavaVersion.setBorder(BorderFactory.createEmptyBorder());
        txtContainer.setBorder(BorderFactory.createEmptyBorder());

        txtResourceGroup.setBackground(null);
        txtStatus.setBackground(null);
        txtLocation.setBackground(null);
        txtSubscription.setBackground(null);
        txtAppServicePlan.setBackground(null);
        txtPricingTier.setBackground(null);
        txtJavaVersion.setBackground(null);
        txtContainer.setBackground(null);
    }

    private void $$$setupUI$$$() {
    }
}
