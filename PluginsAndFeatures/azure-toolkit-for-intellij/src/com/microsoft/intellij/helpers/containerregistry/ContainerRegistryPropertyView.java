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
package com.microsoft.intellij.helpers.containerregistry;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.ui.containerregistry.ContainerRegistryProperty;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.helpers.base.BaseEditor;
import com.microsoft.intellij.runner.container.utils.DockerUtil;
import com.microsoft.intellij.ui.components.AzureActionListenerWrapper;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyViewPresenter;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerRegistryPropertyView extends BaseEditor implements ContainerRegistryPropertyMvpView {

    public static final String ID = ContainerRegistryPropertyView.class.getName();
    private static final String INSIGHT_NAME = "AzurePlugin.IntelliJ.Editor.ContainerRegistryExplorer";
    private final ContainerRegistryPropertyViewPresenter<ContainerRegistryPropertyView> containerPropertyPresenter;
    private final StatusBar statusBar;

    private static final String REFRESH = "Refresh";
    private static final String PREVIOUS_PAGE = "Previous page";
    private static final String NEXT_PAGE = "Next page";
    private static final String TAG = "Tag";
    private static final String REPOSITORY = "Repository";
    private static final String PROPERTY = "Properties";
    private static final String TABLE_LOADING_MESSAGE = "Loading...";
    private static final String TABLE_EMPTY_MESSAGE = "No available items.";
    private static final String ADMIN_NOT_ENABLED = "Admin user is not enabled.";
    private static final String PULL_IMAGE = "Pull Image";
    private static final String DISPLAY_ID = "Azure Plugin";
    private static final String IMAGE_PULL_SUCCESS = "%s is successfully pulled.";
    private static final String REPO_TAG_NOT_AVAILABLE = "Cannot get Current repository and tag";

    private boolean isAdminEnabled;
    private String registryId = "";
    private String subscriptionId = "";
    private String password = "";
    private String password2 = "";
    private String currentRepo;
    private String currentTag;

    private JPanel pnlMain;
    private JTextField txtName;
    private JTextField txtResGrp;
    private JTextField txtSubscription;
    private JTextField txtRegion;
    private JTextField txtServerUrl;
    private JTextField txtUserName;
    private JButton btnPrimaryPassword;
    private JButton btnSecondaryPassword;
    private JTextField txtType;
    private JLabel lblSecondaryPwd;
    private JLabel lblPrimaryPwd;
    private JLabel lblUserName;
    private JButton btnEnable;
    private JButton btnDisable;
    private JPanel pnlPropertyHolder;
    private JPanel pnlProperty;
    private JPanel pnlRepoTable;
    private JPanel pnlTagTable;
    private JBTable tblRepo;
    private JBTable tblTag;
    private AnActionButton btnRepoRefresh;
    private AnActionButton btnRepoPrevious;
    private AnActionButton btnRepoNext;
    private AnActionButton btnTagRefresh;
    private AnActionButton btnTagPrevious;
    private AnActionButton btnTagNext;
    private JPopupMenu menu;

    /**
     * Constructor of ACR property view.
     */
    public ContainerRegistryPropertyView(@NotNull Project project) {
        this.containerPropertyPresenter = new ContainerRegistryPropertyViewPresenter<>();
        this.containerPropertyPresenter.onAttachView(this);
        statusBar = WindowManager.getInstance().getStatusBar(project);

        disableTxtBoard();
        makeTxtOpaque();

        btnPrimaryPassword.addActionListener(event -> {
            try {
                Utils.copyToSystemClipboard(password);
            } catch (Exception e) {
                onError(e.getMessage());
            }
        });

        btnSecondaryPassword.addActionListener(event -> {
            try {
                Utils.copyToSystemClipboard(password2);
            } catch (Exception e) {
                onError(e.getMessage());
            }
        });

        btnEnable.addActionListener(actionEvent -> {
            disableWidgets(true, true);
            onAdminUserBtnClick();
        });
        btnDisable.addActionListener(actionEvent -> {
            disableWidgets(false, false);
            onAdminUserBtnClick();
        });

        HideableDecorator propertyDecorator = new HideableDecorator(pnlPropertyHolder,
                PROPERTY, false /*adjustWindow*/);
        propertyDecorator.setContentComponent(pnlProperty);
        propertyDecorator.setOn(true);

        menu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem(PULL_IMAGE);
        menuItem.addActionListener(new AzureActionListenerWrapper(INSIGHT_NAME, "menuItem", null) {
            @Override
            protected void actionPerformedFunc(ActionEvent e) {
                pullImage();
            }
        });
        menu.add(menuItem);
        disableWidgets(true, true);
    }

    @Override
    public void onErrorWithException(String message, Exception ex) {
        ContainerRegistryPropertyMvpView.super.onErrorWithException(message, ex);
        this.enableWidgets();
    }

    private void createUIComponents() {
        DefaultTableModel repoModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        repoModel.addColumn(REPOSITORY);

        tblRepo = new JBTable(repoModel);
        tblRepo.getEmptyText().setText(TABLE_LOADING_MESSAGE);
        tblRepo.setRowSelectionAllowed(true);
        tblRepo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblRepo.setStriped(true);
        tblRepo.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int selectedRow = tblRepo.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= tblRepo.getRowCount()) {
                return;
            }
            String selectedRepo = (String) tblRepo.getModel().getValueAt(selectedRow, 0);
            if (Utils.isEmptyString(selectedRepo) || Comparing.equal(selectedRepo, currentRepo)) {
                return;
            }
            currentRepo = selectedRepo;
            disableWidgets(false, true);
            tblTag.getEmptyText().setText(TABLE_LOADING_MESSAGE);
            containerPropertyPresenter.onListTags(subscriptionId, registryId, currentRepo, true /*isNextPage*/);
        });

        btnRepoRefresh = new AnActionButton(REFRESH, AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                disableWidgets(true, true);
                tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
                containerPropertyPresenter.onRefreshRepositories(subscriptionId, registryId, true /*isNextPage*/);
            }
        };

        btnRepoPrevious = new AnActionButton(PREVIOUS_PAGE, AllIcons.Actions.MoveUp) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                disableWidgets(true, true);
                tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
                containerPropertyPresenter.onListRepositories(subscriptionId, registryId, false /*isNextPage*/);
            }
        };

        btnRepoNext = new AnActionButton(NEXT_PAGE, AllIcons.Actions.MoveDown) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                disableWidgets(true, true);
                tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
                containerPropertyPresenter.onListRepositories(subscriptionId, registryId, true /*isNextPage*/);
            }
        };

        ToolbarDecorator repoDecorator = ToolbarDecorator.createDecorator(tblRepo)
                .addExtraActions(btnRepoRefresh)
                .addExtraAction(btnRepoPrevious)
                .addExtraAction(btnRepoNext)
                .setToolbarPosition(ActionToolbarPosition.BOTTOM)
                .setToolbarBorder(JBUI.Borders.empty());

        pnlRepoTable = repoDecorator.createPanel();

        DefaultTableModel tagModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tagModel.addColumn(TAG);

        tblTag = new JBTable(tagModel);
        tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
        tblTag.setRowSelectionAllowed(true);
        tblTag.setStriped(true);
        tblTag.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblTag.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int rowIndex = tblTag.getSelectedRow();
                    if (rowIndex < 0 || rowIndex >= tblTag.getRowCount()) {
                        return;
                    }
                    currentTag = (String) tblTag.getModel().getValueAt(rowIndex, 0);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        btnTagRefresh = new AnActionButton(REFRESH, AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                if (Utils.isEmptyString(currentRepo)) {
                    return;
                }
                disableWidgets(false, true);
                containerPropertyPresenter.onListTags(subscriptionId, registryId, currentRepo, true /*isNextPage*/);
            }
        };

        btnTagPrevious = new AnActionButton(PREVIOUS_PAGE, AllIcons.Actions.MoveUp) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                if (Utils.isEmptyString(currentRepo)) {
                    return;
                }
                disableWidgets(false, true);
                containerPropertyPresenter.onListTags(subscriptionId, registryId, currentRepo, false /*isNextPage*/);
            }
        };

        btnTagNext = new AnActionButton(NEXT_PAGE, AllIcons.Actions.MoveDown) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                if (Utils.isEmptyString(currentRepo)) {
                    return;
                }
                disableWidgets(false, true);
                containerPropertyPresenter.onListTags(subscriptionId, registryId, currentRepo, true /*isNextPage*/);
            }
        };

        ToolbarDecorator tagDecorator = ToolbarDecorator.createDecorator(tblTag)
                .addExtraActions(btnTagRefresh)
                .addExtraAction(btnTagPrevious)
                .addExtraAction(btnTagNext)
                .setToolbarPosition(ActionToolbarPosition.BOTTOM)
                .setToolbarBorder(JBUI.Borders.empty());

        pnlTagTable = tagDecorator.createPanel();
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
        containerPropertyPresenter.onDetachView();
    }

    @Override
    public void onReadProperty(String sid, String id) {
        containerPropertyPresenter.onGetRegistryProperty(sid, id);
    }


    @Override
    public void showProperty(ContainerRegistryProperty property) {
        registryId = property.getId();
        subscriptionId = property.getSubscriptionId();
        isAdminEnabled = property.isAdminEnabled();

        txtName.setText(property.getName());
        txtType.setText(property.getType());
        txtResGrp.setText(property.getGroupName());
        txtSubscription.setText(subscriptionId);
        txtRegion.setText(property.getRegionName());
        txtServerUrl.setText(property.getLoginServerUrl());

        lblUserName.setVisible(isAdminEnabled);
        txtUserName.setVisible(isAdminEnabled);
        lblPrimaryPwd.setVisible(isAdminEnabled);
        btnPrimaryPassword.setVisible(isAdminEnabled);
        lblSecondaryPwd.setVisible(isAdminEnabled);
        btnSecondaryPassword.setVisible(isAdminEnabled);
        disableWidgets(true, true);
        if (isAdminEnabled) {
            txtUserName.setText(property.getUserName());
            password = property.getPassword();
            password2 = property.getPassword2();
            tblRepo.getEmptyText().setText(TABLE_LOADING_MESSAGE);
            tblTag.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
            containerPropertyPresenter.onRefreshRepositories(subscriptionId, registryId, true /*isNextPage*/);
        } else {
            tblRepo.getEmptyText().setText(ADMIN_NOT_ENABLED);
            tblTag.getEmptyText().setText(ADMIN_NOT_ENABLED);
        }
        updateAdminUserBtn(isAdminEnabled);
        pnlProperty.revalidate();
    }

    @Override
    public void listRepo(List<String> repos) {
        fillTable(repos, tblRepo);
        enableWidgets();
    }

    @Override
    public void listTag(List<String> tags) {
        fillTable(tags, tblTag);
        enableWidgets();
    }

    private void onAdminUserBtnClick() {
        this.containerPropertyPresenter.onEnableAdminUser(subscriptionId, registryId, !isAdminEnabled);
    }

    private void updateAdminUserBtn(boolean isAdminEnabled) {
        btnEnable.setEnabled(!isAdminEnabled);
        btnDisable.setEnabled(isAdminEnabled);
    }

    private void disableTxtBoard() {
        txtName.setBorder(BorderFactory.createEmptyBorder());
        txtType.setBorder(BorderFactory.createEmptyBorder());
        txtResGrp.setBorder(BorderFactory.createEmptyBorder());
        txtSubscription.setBorder(BorderFactory.createEmptyBorder());
        txtRegion.setBorder(BorderFactory.createEmptyBorder());
        txtServerUrl.setBorder(BorderFactory.createEmptyBorder());
        txtUserName.setBorder(BorderFactory.createEmptyBorder());
    }

    private void makeTxtOpaque() {
        txtName.setBackground(null);
        txtType.setBackground(null);
        txtResGrp.setBackground(null);
        txtSubscription.setBackground(null);
        txtRegion.setBackground(null);
        txtServerUrl.setBackground(null);
        txtUserName.setBackground(null);
    }

    private void fillTable(List<String> list, @NotNull JBTable table) {
        if (list != null && !list.isEmpty()) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.getDataVector().clear();
            list.stream().sorted().forEach(item -> model.addRow(new String[]{item}));
        } else {
            table.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
        }
    }

    private void disableWidgets(boolean needResetRepo, boolean needResetTag) {
        btnEnable.setEnabled(false);
        btnDisable.setEnabled(false);
        tblRepo.setEnabled(false);
        btnRepoRefresh.setEnabled(false);
        btnRepoNext.setEnabled(false);
        btnRepoPrevious.setEnabled(false);
        tblTag.setEnabled(false);
        btnTagRefresh.setEnabled(false);
        btnTagNext.setEnabled(false);
        btnTagPrevious.setEnabled(false);
        if (needResetRepo) {
            currentRepo = null;
            cleanTableData((DefaultTableModel) tblRepo.getModel());
        }
        if (needResetTag) {
            currentTag = null;
            cleanTableData((DefaultTableModel) tblTag.getModel());
        }
    }

    private void enableWidgets() {
        updateAdminUserBtn(isAdminEnabled);
        tblRepo.setEnabled(true);
        btnRepoRefresh.setEnabled(true);
        if (containerPropertyPresenter.hasNextRepoPage()) {
            btnRepoNext.setEnabled(true);
        }
        if (containerPropertyPresenter.hasPreviousRepoPage()) {
            btnRepoPrevious.setEnabled(true);
        }
        tblTag.setEnabled(true);
        if (currentRepo == null) {
            return;
        }
        btnTagRefresh.setEnabled(true);
        if (containerPropertyPresenter.hasNextTagPage()) {
            btnTagNext.setEnabled(true);
        }
        if (containerPropertyPresenter.hasPreviousTagPage()) {
            btnTagPrevious.setEnabled(true);
        }
    }

    private void pullImage() {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, PULL_IMAGE, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    if (Utils.isEmptyString(currentRepo) || Utils.isEmptyString(currentTag)) {
                        throw new Exception(REPO_TAG_NOT_AVAILABLE);
                    }
                    final Registry registry = ContainerRegistryMvpModel.getInstance()
                            .getContainerRegistry(subscriptionId, registryId);
                    final PrivateRegistryImageSetting setting = ContainerRegistryMvpModel.getInstance()
                            .createImageSettingWithRegistry(registry);
                    final String image = String.format("%s:%s", currentRepo, currentTag);
                    final String fullImageTagName = String.format("%s/%s", registry.loginServerUrl(), image);
                    DockerClient docker = DefaultDockerClient.fromEnv().build();
                    DockerUtil.pullImage(docker, registry.loginServerUrl(), setting.getUsername(),
                            setting.getPassword(), fullImageTagName);
                    String message = String.format(IMAGE_PULL_SUCCESS, fullImageTagName);
                    UIUtils.showNotification(statusBar, message, MessageType.INFO);
                    sendTelemetry(true, subscriptionId, null);
                } catch (Exception e) {
                    UIUtils.showNotification(statusBar, e.getMessage(), MessageType.ERROR);
                    sendTelemetry(false, subscriptionId, e.getMessage());
                }
            }
        });
    }

    private void cleanTableData(DefaultTableModel model) {
        model.getDataVector().clear();
        model.fireTableDataChanged();
    }

    private void sendTelemetry(boolean success, @NotNull String subscriptionId, @Nullable String errorMsg) {
        Map<String, String> map = new HashMap<>();
        map.put("SubscriptionId", subscriptionId);
        map.put("Success", String.valueOf(success));
        if (!success) {
            map.put("ErrorMsg", errorMsg);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "ACR", PULL_IMAGE, map);
    }
}
