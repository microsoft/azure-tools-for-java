/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.docker.webapponlinux.ui;

import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;
import com.microsoft.azure.toolkit.intellij.webapp.docker.ContainerSettingPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.webapp.docker.utils.Constant;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import icons.MavenIcons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azure.toolkit.intellij.webapp.docker.utils.DockerUtil;
import com.microsoft.azure.toolkit.intellij.webapp.docker.webapponlinux.WebAppOnLinuxDeployConfiguration;

import com.microsoft.tooling.msservices.serviceexplorer.azure.container.WebAppOnLinuxDeployPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.WebAppOnLinuxDeployView;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import java.awt.event.ItemEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import static com.microsoft.azure.toolkit.intellij.webapp.docker.pushimage.ui.SettingPanel.PRIVATE_DOCKER_REGISTRY;

public class SettingPanel extends AzureSettingPanel<WebAppOnLinuxDeployConfiguration> implements WebAppOnLinuxDeployView {
    private static final String NOT_APPLICABLE = "N/A";
    private static final String TABLE_HEAD_WEB_APP_NAME = "Name";
    private static final String TABLE_HEAD_RESOURCE_GROUP = "Resource Group";
    private static final String TABLE_LOADING_MESSAGE = "Loading ... ";
    private static final String TABLE_EMPTY_MESSAGE = "No available Web App for Containers.";
    private static final String APP_NAME_PREFIX = "webapp-linux";
    private static final String RESOURCE_GROUP_NAME_PREFIX = "rg-web-linux";
    private static final String APP_SERVICE_PLAN_NAME_PREFIX = "appsp-linux-";
    private static final String TITLE_RESOURCE_GROUP = "&Resource Group";
    private static final String TITLE_APP_SERVICE_PLAN = "App Service &Plan";
    private static final String TITLE_ACR = "Azure &Container Registry";
    private static final String TITLE_WEB_APP = "&Web App for Containers";

    private final WebAppOnLinuxDeployPresenter<SettingPanel> webAppOnLinuxDeployPresenter;
    private JTextField textAppName;
    private JComboBox<Subscription> comboSubscription;
    private JComboBox<ResourceGroup> comboResourceGroup;
    private JPanel pnlUpdate;
    private JPanel rootPanel;
    private JPanel pnlWebAppOnLinuxTable;
    private JRadioButton rdoUseExist;
    private JRadioButton rdoCreateNew;
    private JPanel pnlCreate;
    private JComboBox<Region> cbLocation;
    private JComboBox<PricingTier> cbPricing;
    private JRadioButton rdoCreateResGrp;
    private JTextField txtNewResGrp;
    private JRadioButton rdoUseExistResGrp;
    private JRadioButton rdoCreateAppServicePlan;
    private JTextField txtCreateAppServicePlan;
    private JRadioButton rdoUseExistAppServicePlan;
    private JComboBox<IAppServicePlan> cbExistAppServicePlan;
    private JLabel lblLocation;
    private JLabel lblPricing;
    private JPanel pnlAcr;
    private JPanel pnlWebApp;
    private JBTable webAppTable;
    private AnActionButton btnRefresh;
    private List<IWebApp> cachedWebAppList;
    private String defaultWebAppId;
    private String defaultLocationName;
    private String defaultPricingTier;
    private String defaultResourceGroup;
    private String defaultSubscriptionId;
    private String defaultAppServicePlanId;
    private JTextField textSelectedAppName; // invisible, used to trigger validation on tableRowSelection
    private JComboBox<Artifact> cbArtifact;
    private JLabel lblArtifact;
    private JPanel pnlArtifact;
    private JPanel pnlResourceGroupHolder;
    private JPanel pnlAppServicePlanHolder;
    private JPanel pnlResourceGroup;
    private JPanel pnlAppServicePlan;
    private JPanel pnlAcrHolder;
    private JPanel pnlWebAppHolder;
    private ContainerSettingPanel containerSettingPanel;
    private JPanel pnlMavenProject;
    private JLabel lblMavenProject;
    private JComboBox<MavenProject> cbMavenProject;

    /**
     * Constructor.
     */

    public SettingPanel(Project project) {
        super(project);
        webAppOnLinuxDeployPresenter = new WebAppOnLinuxDeployPresenter<>();
        webAppOnLinuxDeployPresenter.onAttachView(this);
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.

        // set create/update panel visible
        updatePanelVisibility();
        rdoCreateNew.addActionListener(e -> updatePanelVisibility());
        rdoUseExist.addActionListener(e -> updatePanelVisibility());

        // resource group
        comboResourceGroup.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(JList jlist, ResourceGroup resourceGroup, int index, boolean isSelected, boolean
                    cellHasFocus) {
                if (resourceGroup != null) {
                    setText(resourceGroup.getName());
                }
            }
        });
        comboResourceGroup.addItemListener(this::onComboResourceGroupSelection);
        updateResourceGroupEnabled();
        rdoCreateResGrp.addActionListener(e -> updateResourceGroupEnabled());
        rdoUseExistResGrp.addActionListener(e -> updateResourceGroupEnabled());

        // subscription combo
        comboSubscription.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(JList jlist, Subscription subscription, int index, boolean isSelected, boolean
                    cellHasFocus) {
                if (subscription != null) {
                    setText(String.format("%s (%s)", subscription.getName(), subscription.getId()));
                }
            }
        });

        comboSubscription.addItemListener(this::onComboSubscriptionSelection);

        // app service plan
        cbExistAppServicePlan.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(JList jlist, IAppServicePlan asp, int index, boolean isSelected, boolean
                    cellHasFocus) {
                if (asp != null) {
                    setText(asp.name());
                }
            }
        });
        cbExistAppServicePlan.addItemListener(this::onComboExistingAspSelection);
        updateAppServicePlanEnabled();
        rdoCreateAppServicePlan.addActionListener(e -> updateAppServicePlanEnabled());
        rdoUseExistAppServicePlan.addActionListener(e -> updateAppServicePlanEnabled());

        // location combo
        cbLocation.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(JList jlist, Region location, int index, boolean isSelected, boolean cellHasFocus) {
                if (location != null) {
                    setText(location.getName());
                }
            }
        });

        // pricing tier combo
        cbPricing.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(JList jlist, PricingTier pricingTier, int index, boolean isSelected, boolean
                    cellHasFocus) {
                if (pricingTier != null) {
                    setText(pricingTier.toString());
                }
            }
        });

        cbArtifact.addActionListener(e -> artifactActionPerformed((Artifact) cbArtifact.getSelectedItem()));

        cbArtifact.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(JList jlist, Artifact artifact, int i, boolean b, boolean b1) {
                if (artifact != null) {
                    setIcon(artifact.getArtifactType().getIcon());
                    setText(artifact.getName());
                }
            }
        });

        cbMavenProject.addActionListener(e -> {
            final MavenProject selectedMavenProject = (MavenProject) cbMavenProject.getSelectedItem();
            if (selectedMavenProject != null) {
                containerSettingPanel.setDockerPath(
                        DockerUtil.getDefaultDockerFilePathIfExist(selectedMavenProject.getDirectory())
                );
            }
        });

        cbMavenProject.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(JList list, MavenProject mavenProject, int i, boolean b, boolean b1) {
                if (mavenProject != null) {
                    setIcon(MavenIcons.MavenProject);
                    setText(mavenProject.toString());
                }
            }
        });

        // fold sub panel
        final HideableDecorator resGrpDecorator = new HideableDecorator(pnlResourceGroupHolder,
                TITLE_RESOURCE_GROUP, true /*adjustWindow*/);
        resGrpDecorator.setContentComponent(pnlResourceGroup);
        resGrpDecorator.setOn(true);

        final HideableDecorator appServicePlanDecorator = new HideableDecorator(pnlAppServicePlanHolder,
                TITLE_APP_SERVICE_PLAN, true /*adjustWindow*/);
        appServicePlanDecorator.setContentComponent(pnlAppServicePlan);
        appServicePlanDecorator.setOn(true);

        final HideableDecorator acrDecorator = new HideableDecorator(pnlAcrHolder,
                TITLE_ACR, true /*adjustWindow*/);
        acrDecorator.setContentComponent(pnlAcr);
        acrDecorator.setOn(true);

        final HideableDecorator webAppDecorator = new HideableDecorator(pnlWebAppHolder,
                TITLE_WEB_APP, true /*adjustWindow*/);
        webAppDecorator.setContentComponent(pnlWebApp);
        webAppDecorator.setOn(true);

        containerSettingPanel.setStartupFileVisible(true);
        containerSettingPanel.onListRegistries();
    }

    @Override
    @NotNull
    public String getPanelName() {
        return "Run On Web App for Containers";
    }

    @Override
    @NotNull
    public JPanel getMainPanel() {
        return rootPanel;
    }

    @Override
    @NotNull
    protected JComboBox<Artifact> getCbArtifact() {
        return cbArtifact;
    }

    @Override
    @NotNull
    protected JLabel getLblArtifact() {
        return lblArtifact;
    }

    @Override
    @NotNull
    protected JComboBox<MavenProject> getCbMavenProject() {
        return cbMavenProject;
    }

    @Override
    @NotNull
    protected JLabel getLblMavenProject() {
        return lblMavenProject;
    }

    private void onComboResourceGroupSelection(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            cbExistAppServicePlan.removeAllItems();
            lblLocation.setText("");
            lblPricing.setText("");
            final Subscription sub = (Subscription) comboSubscription.getSelectedItem();
            final ResourceGroup rg = (ResourceGroup) comboResourceGroup.getSelectedItem();
            if (sub != null && rg != null) {
                updateAppServicePlanList(sub.getId(), rg.getName());
            }
        }
    }

    private void onComboExistingAspSelection(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            final AppServicePlan asp = (AppServicePlan) cbExistAppServicePlan.getSelectedItem();
            if (asp != null) {
                lblLocation.setText(asp.entity().getRegion());
                // TODO(andxu): change to toString
                lblPricing.setText(asp.entity().getPricingTier().getSize());
            }
        }
    }

    private void updateAppServicePlanEnabled() {
        cbExistAppServicePlan.setEnabled(rdoUseExistAppServicePlan.isSelected());
        txtCreateAppServicePlan.setEnabled(rdoCreateAppServicePlan.isSelected());
        cbLocation.setEnabled(rdoCreateAppServicePlan.isSelected());
        cbPricing.setEnabled(rdoCreateAppServicePlan.isSelected());
    }

    private void onComboSubscriptionSelection(ItemEvent event) {
        if (event.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        comboResourceGroup.removeAllItems();
        cbLocation.removeAllItems();
        final Subscription sb = (Subscription) comboSubscription.getSelectedItem();
        if (sb != null) {
            updateResourceGroupList(sb.getId());
            updateLocationList(sb.getId());
        }
    }

    /**
     * Function triggered by any content change events.
     *
     * @param webAppOnLinuxDeployConfiguration configuration instance
     */
    @Override
    public void apply(WebAppOnLinuxDeployConfiguration webAppOnLinuxDeployConfiguration) {
        webAppOnLinuxDeployConfiguration.setDockerFilePath(containerSettingPanel.getDockerPath());
        // set ACR info
        webAppOnLinuxDeployConfiguration.setPrivateRegistryImageSetting(new PrivateRegistryImageSetting(
                containerSettingPanel.getServerUrl().replaceFirst("^https?://", "").replaceFirst("/$", ""),
                containerSettingPanel.getUserName(),
                containerSettingPanel.getPassword(),
                containerSettingPanel.getImageTag(),
                containerSettingPanel.getStartupFile()
        ));
        final ISecureStore secureStore = AzureStoreManager.getInstance().getSecureStore();
        secureStore.savePassword(PRIVATE_DOCKER_REGISTRY, containerSettingPanel.getServerUrl(), containerSettingPanel.getUserName(),
                containerSettingPanel.getPassword());

        webAppOnLinuxDeployConfiguration.setTargetPath(getTargetPath());
        webAppOnLinuxDeployConfiguration.setTargetName(getTargetName());

        // set web app info
        if (rdoUseExist.isSelected()) {
            // existing web app
            webAppOnLinuxDeployConfiguration.setCreatingNewWebAppOnLinux(false);
            IWebApp selectedWebApp = null;
            final int index = webAppTable.getSelectedRow();
            if (cachedWebAppList != null && index >= 0 && index < cachedWebAppList.size()) {
                selectedWebApp = cachedWebAppList.get(webAppTable.getSelectedRow());
            }
            if (selectedWebApp != null) {
                webAppOnLinuxDeployConfiguration.setWebAppId(selectedWebApp.id());
                webAppOnLinuxDeployConfiguration.setAppName(selectedWebApp.name());
                webAppOnLinuxDeployConfiguration.setSubscriptionId(selectedWebApp.subscriptionId());
                webAppOnLinuxDeployConfiguration.setResourceGroupName(selectedWebApp.resourceGroup());
            } else {
                webAppOnLinuxDeployConfiguration.setWebAppId(null);
                webAppOnLinuxDeployConfiguration.setAppName(null);
                webAppOnLinuxDeployConfiguration.setSubscriptionId(null);
                webAppOnLinuxDeployConfiguration.setResourceGroupName(null);
            }
        } else if (rdoCreateNew.isSelected()) {
            // create new web app
            webAppOnLinuxDeployConfiguration.setCreatingNewWebAppOnLinux(true);
            webAppOnLinuxDeployConfiguration.setWebAppId("");
            webAppOnLinuxDeployConfiguration.setAppName(textAppName.getText());
            final Subscription selectedSubscription = (Subscription) comboSubscription.getSelectedItem();
            if (selectedSubscription != null) {
                webAppOnLinuxDeployConfiguration.setSubscriptionId(selectedSubscription.getId());
            }

            // resource group
            if (rdoUseExistResGrp.isSelected()) {
                // existing RG
                webAppOnLinuxDeployConfiguration.setCreatingNewResourceGroup(false);
                final ResourceGroup selectedRg = (ResourceGroup) comboResourceGroup.getSelectedItem();
                if (selectedRg != null) {
                    webAppOnLinuxDeployConfiguration.setResourceGroupName(selectedRg.getName());
                } else {
                    webAppOnLinuxDeployConfiguration.setResourceGroupName(null);
                }
            } else if (rdoCreateResGrp.isSelected()) {
                // new RG
                webAppOnLinuxDeployConfiguration.setCreatingNewResourceGroup(true);
                webAppOnLinuxDeployConfiguration.setResourceGroupName(txtNewResGrp.getText());
            }

            // app service plan
            if (rdoCreateAppServicePlan.isSelected()) {
                webAppOnLinuxDeployConfiguration.setCreatingNewAppServicePlan(true);
                webAppOnLinuxDeployConfiguration.setAppServicePlanName(txtCreateAppServicePlan.getText());
                final Region selectedLocation = (Region) cbLocation.getSelectedItem();
                if (selectedLocation != null) {
                    webAppOnLinuxDeployConfiguration.setLocationName(selectedLocation.getName());
                } else {
                    webAppOnLinuxDeployConfiguration.setLocationName(null);
                }

                final PricingTier selectedPricingTier = (PricingTier) cbPricing.getSelectedItem();
                if (selectedPricingTier != null) {
                    webAppOnLinuxDeployConfiguration.setPricingSkuTier(selectedPricingTier.getTier());
                    webAppOnLinuxDeployConfiguration.setPricingSkuSize(selectedPricingTier.getSize());
                } else {
                    webAppOnLinuxDeployConfiguration.setPricingSkuTier(null);
                    webAppOnLinuxDeployConfiguration.setPricingSkuSize(null);
                }
            } else if (rdoUseExistAppServicePlan.isSelected()) {
                webAppOnLinuxDeployConfiguration.setCreatingNewAppServicePlan(false);
                final AppServicePlan selectedAsp = (AppServicePlan) cbExistAppServicePlan.getSelectedItem();
                if (selectedAsp != null) {
                    webAppOnLinuxDeployConfiguration.setAppServicePlanId(selectedAsp.id());
                } else {
                    webAppOnLinuxDeployConfiguration.setAppServicePlanId(null);
                }
            }
        }
    }

    /**
     * Function triggered in constructing the panel.
     *
     * @param conf configuration instance
     */
    @Override
    public void resetFromConfig(@NotNull WebAppOnLinuxDeployConfiguration conf) {
        if (!isMavenProject()) {
            containerSettingPanel.setDockerPath(DockerUtil.getDefaultDockerFilePathIfExist(getProjectBasePath()));
        }

        // load dockerFile path from existing configuration.
        if (!Utils.isEmptyString(conf.getDockerFilePath())) {
            containerSettingPanel.setDockerPath(conf.getDockerFilePath());
        }

        final PrivateRegistryImageSetting acrInfo = conf.getPrivateRegistryImageSetting();
        if (StringUtils.isNotEmpty(acrInfo.getServerUrl())) {
            final ISecureStore secureStore = AzureStoreManager.getInstance().getSecureStore();
            secureStore.migratePassword(acrInfo.getServerUrl(), acrInfo.getUsername(), PRIVATE_DOCKER_REGISTRY, acrInfo.getServerUrl(), acrInfo.getUsername());
            acrInfo.setPassword(secureStore.loadPassword(PRIVATE_DOCKER_REGISTRY, acrInfo.getServerUrl(), acrInfo.getUsername()));
        }
        containerSettingPanel.setTxtFields(acrInfo);

        // cache for table/combo selection
        defaultSubscriptionId = conf.getSubscriptionId();
        defaultWebAppId = conf.getWebAppId();
        defaultLocationName = conf.getLocationName();
        defaultPricingTier = StringUtils.isEmpty(conf.getPricingSkuTier()) ?
                Constant.WEBAPP_CONTAINER_DEFAULT_PRICING_TIER :
                new PricingTier(conf.getPricingSkuTier(), conf.getPricingSkuSize()).toString();
        defaultResourceGroup = conf.getResourceGroupName();
        defaultAppServicePlanId = conf.getAppServicePlanId();

        // pnlUseExisting
        loadWebAppList();

        // pnlCreateNew
        webAppOnLinuxDeployPresenter.onLoadSubscriptionList(); // including related RG & Region
        webAppOnLinuxDeployPresenter.onLoadPricingTierList();

        final boolean creatingRg = conf.isCreatingNewResourceGroup();
        rdoCreateResGrp.setSelected(creatingRg);
        rdoUseExistResGrp.setSelected(!creatingRg);
        updateResourceGroupEnabled();
        if (creatingRg) {
            txtNewResGrp.setText(conf.getResourceGroupName());
        }

        final boolean creatingAsp = conf.isCreatingNewAppServicePlan();
        rdoCreateAppServicePlan.setSelected(creatingAsp);
        rdoUseExistAppServicePlan.setSelected(!creatingAsp);
        updateAppServicePlanEnabled();
        if (creatingAsp) {
            txtCreateAppServicePlan.setText(conf.getAppServicePlanName());
        }

        // active panel
        final boolean creatingApp = conf.isCreatingNewWebAppOnLinux();
        if (creatingApp) {
            textAppName.setText(conf.getAppName());
        }
        rdoCreateNew.setSelected(creatingApp);
        rdoUseExist.setSelected(!creatingApp);
        updatePanelVisibility();

        // default value for new resources
        final DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        final String date = df.format(new Date());
        if (Utils.isEmptyString(textAppName.getText())) {
            textAppName.setText(String.format("%s-%s", APP_NAME_PREFIX, date));
        }
        if (Utils.isEmptyString(txtNewResGrp.getText())) {
            txtNewResGrp.setText(String.format("%s-%s", RESOURCE_GROUP_NAME_PREFIX, date));
        }
        if (Utils.isEmptyString(txtCreateAppServicePlan.getText())) {
            txtCreateAppServicePlan.setText(String.format("%s-%s", APP_SERVICE_PLAN_NAME_PREFIX, date));
        }
    }

    private void loadWebAppList() {
        btnRefresh.setEnabled(false);
        webAppTable.getEmptyText().setText(TABLE_LOADING_MESSAGE);
        webAppOnLinuxDeployPresenter.onLoadAppList();
    }

    private void createUIComponents() {
        containerSettingPanel = new ContainerSettingPanel(project);
        // create table of Web App on Linux
        final DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.addColumn(TABLE_HEAD_WEB_APP_NAME);
        tableModel.addColumn(TABLE_HEAD_RESOURCE_GROUP);
        webAppTable = new JBTable(tableModel);
        webAppTable.getEmptyText().setText(TABLE_LOADING_MESSAGE);
        webAppTable.setRowSelectionAllowed(true);
        webAppTable.setDragEnabled(false);
        webAppTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        webAppTable.getSelectionModel().addListSelectionListener(event -> {
            final int index = webAppTable.getSelectedRow();
            if (cachedWebAppList != null && index >= 0 && index < cachedWebAppList.size()) {
                textSelectedAppName.setText(cachedWebAppList.get(webAppTable.getSelectedRow()).name());
            }
        });
        btnRefresh = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                btnRefresh.setEnabled(false);
                webAppTable.getEmptyText().setText(TABLE_LOADING_MESSAGE);
                final DefaultTableModel model = (DefaultTableModel) webAppTable.getModel();
                model.getDataVector().clear();
                model.fireTableDataChanged();
                textSelectedAppName.setText("");
                webAppOnLinuxDeployPresenter.onRefreshList();
            }
        };
        final ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(webAppTable)
                .addExtraActions(btnRefresh).setToolbarPosition(ActionToolbarPosition.TOP);
        pnlWebAppOnLinuxTable = tableToolbarDecorator.createPanel();

    }

    private void updatePanelVisibility() {
        pnlCreate.setVisible(rdoCreateNew.isSelected());
        pnlUpdate.setVisible(rdoUseExist.isSelected());
    }

    private void updateResourceGroupEnabled() {
        txtNewResGrp.setEnabled(rdoCreateResGrp.isSelected());
        comboResourceGroup.setEnabled(rdoUseExistResGrp.isSelected());
    }

    @Override
    public void renderWebAppOnLinuxList(List<IWebApp> webAppOnLinuxList) {
        btnRefresh.setEnabled(true);
        webAppTable.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
        final List<IWebApp> sortedList = webAppOnLinuxList.stream()
                .sorted((a, b) -> a.subscriptionId().compareToIgnoreCase(b.subscriptionId()))
                .collect(Collectors.toList());
        cachedWebAppList = sortedList;
        if (cachedWebAppList.size() > 0) {
            final DefaultTableModel model = (DefaultTableModel) webAppTable.getModel();
            model.getDataVector().clear();
            for (final IWebApp resource : sortedList) {
                model.addRow(new String[]{resource.name(), resource.resourceGroup()});
            }
        }

        // select active web app
        for (int index = 0; index < cachedWebAppList.size(); index++) {
            if (StringUtils.equals(cachedWebAppList.get(index).id(), defaultWebAppId)) {
                webAppTable.setRowSelectionInterval(index, index);
                // defaultWebAppId = null; // clear to select nothing in future refreshing
                break;
            }
        }
    }

    @Override
    public void renderSubscriptionList(List<Subscription> subscriptions) {
        comboSubscription.removeAllItems();
        if (subscriptions != null && subscriptions.size() > 0) {
            subscriptions.forEach((item) -> {
                comboSubscription.addItem(item);
                if (StringUtils.equals(item.getId(), defaultSubscriptionId)) {
                    comboSubscription.setSelectedItem(item);
                    defaultSubscriptionId = null;
                }
            });
        }
    }

    @Override
    public void renderResourceGroupList(List<ResourceGroup> resourceGroupList) {
        comboResourceGroup.removeAllItems();
        if (resourceGroupList != null && resourceGroupList.size() > 0) {
            resourceGroupList.forEach((item) -> {
                comboResourceGroup.addItem(item);
                if (StringUtils.equals(item.getName(), defaultResourceGroup)) {
                    comboResourceGroup.setSelectedItem(item);
                    // defaultResourceGroup = null;
                }
            });
        }
    }

    @Override
    public void renderLocationList(List<Region> locationList) {
        cbLocation.removeAllItems();
        if (locationList != null && locationList.size() > 0) {
            locationList.forEach((item) -> {
                cbLocation.addItem(item);
                if (StringUtils.equals(item.getName(), defaultLocationName)) {
                    cbLocation.setSelectedItem(item);
                    // defaultLocationName = null;
                }
            });
        }
    }

    @Override
    public void renderAppServicePlanList(List<IAppServicePlan> appServicePlans) {
        cbExistAppServicePlan.removeAllItems();
        lblLocation.setText(NOT_APPLICABLE);
        lblPricing.setText(NOT_APPLICABLE);
        if (appServicePlans != null && appServicePlans.size() > 0) {
            appServicePlans.forEach((item) -> {
                cbExistAppServicePlan.addItem(item);
                if (StringUtils.equals(item.id(), defaultAppServicePlanId)) {
                    cbExistAppServicePlan.setSelectedItem(item);
                    // defaultAppServicePlanId = null;
                }
            });
        }
    }

    @Override
    public void renderPricingTierList(List<PricingTier> pricingTierList) {
        cbPricing.removeAllItems();
        if (pricingTierList != null && pricingTierList.size() > 0) {
            pricingTierList.forEach((item) -> {
                cbPricing.addItem(item);
                if (StringUtils.equals(item.toString(), defaultPricingTier)) {
                    cbPricing.setSelectedItem(item);
                }
            });
        }
    }

    /**
     * Let the presenter release the view. Will be called by:
     */
    @Override
    public void disposeEditor() {
        containerSettingPanel.disposeEditor();
        webAppOnLinuxDeployPresenter.onDetachView();
    }

    private void updateResourceGroupList(String sid) {
        webAppOnLinuxDeployPresenter.onLoadResourceGroup(sid);
    }

    private void updateLocationList(String sid) {
        webAppOnLinuxDeployPresenter.onLoadLocationList(sid);
    }

    private void updateAppServicePlanList(String sid, String rg) {
        webAppOnLinuxDeployPresenter.onLoadAppServicePlan(sid, rg);
    }

    private void updateAppServicePlanList(String sid) {
        // TODO: blocked by SDK, API cannot list Linux ASP now.
        webAppOnLinuxDeployPresenter.onLoadAppServicePlan(sid);
    }

    private void $$$setupUI$$$() {
    }
}
