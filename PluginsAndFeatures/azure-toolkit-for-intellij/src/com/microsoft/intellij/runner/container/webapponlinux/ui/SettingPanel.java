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

package com.microsoft.intellij.runner.container.webapponlinux.ui;

import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.container.utils.Constant;
import icons.MavenIcons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.intellij.runner.container.common.ContainerSettingPanel;
import com.microsoft.intellij.runner.container.utils.DockerUtil;
import com.microsoft.intellij.runner.container.webapponlinux.WebAppOnLinuxDeployConfiguration;

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

public class SettingPanel extends AzureSettingPanel<WebAppOnLinuxDeployConfiguration> implements WebAppOnLinuxDeployView {
    private static final String NOT_APPLICABLE = "N/A";
    private static final String TABLE_HEAD_WEB_APP_NAME = "Name";
    private static final String TABLE_HEAD_RESOURCE_GROUP = "Resource Group";
    private static final String TABLE_LOADING_MESSAGE = "Loading ... ";
    private static final String TABLE_EMPTY_MESSAGE = "No available Web App for Containers.";
    private static final String APP_NAME_PREFIX = "webapp-linux";
    private static final String RESOURCE_GROUP_NAME_PREFIX = "rg-web-linux";
    private static final String APP_SERVICE_PLAN_NAME_PREFIX = "appsp-linux-";
    private static final String TITLE_RESOURCE_GROUP = "Resource Group";
    private static final String TITLE_APP_SERVICE_PLAN = "App Service Plan";
    private static final String TITLE_ACR = "Azure Container Registry";
    private static final String TITLE_WEB_APP = "Web App for Containers";

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
    private JComboBox<Location> cbLocation;
    private JComboBox<PricingTier> cbPricing;
    private JRadioButton rdoCreateResGrp;
    private JTextField txtNewResGrp;
    private JRadioButton rdoUseExistResGrp;
    private JRadioButton rdoCreateAppServicePlan;
    private JTextField txtCreateAppServicePlan;
    private JRadioButton rdoUseExistAppServicePlan;
    private JComboBox<AppServicePlan> cbExistAppServicePlan;
    private JLabel lblLocation;
    private JLabel lblPricing;
    private JPanel pnlAcr;
    private JPanel pnlWebApp;
    private JBTable webAppTable;
    private AnActionButton btnRefresh;
    private List<ResourceEx<WebApp>> cachedWebAppList;
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
        comboResourceGroup.setRenderer(new ListCellRendererWrapper<ResourceGroup>() {
            @Override
            public void customize(JList jlist, ResourceGroup resourceGroup, int index, boolean isSelected, boolean
                    cellHasFocus) {
                if (resourceGroup != null) {
                    setText(resourceGroup.name());
                }
            }
        });
        comboResourceGroup.addItemListener(this::onComboResourceGroupSelection);
        updateResourceGroupEnabled();
        rdoCreateResGrp.addActionListener(e -> updateResourceGroupEnabled());
        rdoUseExistResGrp.addActionListener(e -> updateResourceGroupEnabled());

        // subscription combo
        comboSubscription.setRenderer(new ListCellRendererWrapper<Subscription>() {
            @Override
            public void customize(JList jlist, Subscription subscription, int index, boolean isSelected, boolean
                    cellHasFocus) {
                if (subscription != null) {
                    setText(String.format("%s (%s)", subscription.displayName(), subscription.subscriptionId()));
                }
            }
        });

        comboSubscription.addItemListener(this::onComboSubscriptionSelection);

        // app service plan
        cbExistAppServicePlan.setRenderer(new ListCellRendererWrapper<AppServicePlan>() {
            @Override
            public void customize(JList jlist, AppServicePlan asp, int index, boolean isSelected, boolean
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
        cbLocation.setRenderer(new ListCellRendererWrapper<Location>() {
            @Override
            public void customize(JList jlist, Location location, int index, boolean isSelected, boolean cellHasFocus) {
                if (location != null) {
                    setText(location.displayName());
                }
            }
        });

        // pricing tier combo
        cbPricing.setRenderer(new ListCellRendererWrapper<PricingTier>() {
            @Override
            public void customize(JList jlist, PricingTier pricingTier, int index, boolean isSelected, boolean
                    cellHasFocus) {
                if (pricingTier != null) {
                    setText(pricingTier.toString());
                }
            }
        });

        cbArtifact.addActionListener(e -> {
            artifactActionPeformed((Artifact) cbArtifact.getSelectedItem());
        });

        cbArtifact.setRenderer(new ListCellRendererWrapper<Artifact>() {
            @Override
            public void customize(JList jlist, Artifact artifact, int i, boolean b, boolean b1) {
                if (artifact != null) {
                    setIcon(artifact.getArtifactType().getIcon());
                    setText(artifact.getName());
                }
            }
        });

        cbMavenProject.addActionListener(e -> {
            MavenProject selectedMavenProject = (MavenProject) cbMavenProject.getSelectedItem();
            if (selectedMavenProject != null) {
                containerSettingPanel.setDockerPath(
                        DockerUtil.getDefaultDockerFilePathIfExist(selectedMavenProject.getDirectory())
                );
            }
        });

        cbMavenProject.setRenderer(new ListCellRendererWrapper<MavenProject>() {
            @Override
            public void customize(JList jList, MavenProject mavenProject, int i, boolean b, boolean b1) {
                if (mavenProject != null) {
                    setIcon(MavenIcons.MavenProject);
                    setText(mavenProject.toString());
                }
            }
        });

        // fold sub panel
        HideableDecorator resGrpDecorator = new HideableDecorator(pnlResourceGroupHolder,
                TITLE_RESOURCE_GROUP, true /*adjustWindow*/);
        resGrpDecorator.setContentComponent(pnlResourceGroup);
        resGrpDecorator.setOn(true);

        HideableDecorator appServicePlanDecorator = new HideableDecorator(pnlAppServicePlanHolder,
                TITLE_APP_SERVICE_PLAN, true /*adjustWindow*/);
        appServicePlanDecorator.setContentComponent(pnlAppServicePlan);
        appServicePlanDecorator.setOn(true);

        HideableDecorator acrDecorator = new HideableDecorator(pnlAcrHolder,
                TITLE_ACR, true /*adjustWindow*/);
        acrDecorator.setContentComponent(pnlAcr);
        acrDecorator.setOn(true);

        HideableDecorator webAppDecorator = new HideableDecorator(pnlWebAppHolder,
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
            Subscription sub = (Subscription) comboSubscription.getSelectedItem();
            ResourceGroup rg = (ResourceGroup) comboResourceGroup.getSelectedItem();
            if (sub != null && rg != null) {
                updateAppServicePlanList(sub.subscriptionId(), rg.name());
            }
        }
    }

    private void onComboExistingAspSelection(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            AppServicePlan asp = (AppServicePlan) cbExistAppServicePlan.getSelectedItem();
            if (asp != null) {
                lblLocation.setText(asp.regionName());
                lblPricing.setText(asp.pricingTier().toString());
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
        Subscription sb = (Subscription) comboSubscription.getSelectedItem();
        if (sb != null) {
            updateResourceGroupList(sb.subscriptionId());
            updateLocationList(sb.subscriptionId());
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
        PrivateRegistryImageSetting privateRegistryImageSetting = new PrivateRegistryImageSetting(
            containerSettingPanel.getServerUrl().replaceFirst("^https?://", "").replaceFirst("/$", ""),
            containerSettingPanel.getUserName(),
            containerSettingPanel.getPassword(),
            containerSettingPanel.getImageTag(),
            containerSettingPanel.getStartupFile()
        );
        webAppOnLinuxDeployConfiguration.setPrivateRegistryImageSetting(privateRegistryImageSetting);
        savePassword(containerSettingPanel.getServerUrl(), containerSettingPanel.getUserName(),
            containerSettingPanel.getPassword());

        webAppOnLinuxDeployConfiguration.setTargetPath(getTargetPath());
        webAppOnLinuxDeployConfiguration.setTargetName(getTargetName());

        // set web app info
        if (rdoUseExist.isSelected()) {
            // existing web app
            webAppOnLinuxDeployConfiguration.setCreatingNewWebAppOnLinux(false);
            ResourceEx<WebApp> selectedWebApp = null;
            int index = webAppTable.getSelectedRow();
            if (cachedWebAppList != null && index >= 0 && index < cachedWebAppList.size()) {
                selectedWebApp = cachedWebAppList.get(webAppTable.getSelectedRow());
            }
            if (selectedWebApp != null) {
                webAppOnLinuxDeployConfiguration.setWebAppId(selectedWebApp.getResource().id());
                webAppOnLinuxDeployConfiguration.setAppName(selectedWebApp.getResource().name());
                webAppOnLinuxDeployConfiguration.setSubscriptionId(selectedWebApp.getSubscriptionId());
                webAppOnLinuxDeployConfiguration.setResourceGroupName(selectedWebApp.getResource().resourceGroupName());
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
            Subscription selectedSubscription = (Subscription) comboSubscription.getSelectedItem();
            if (selectedSubscription != null) {
                webAppOnLinuxDeployConfiguration.setSubscriptionId(selectedSubscription.subscriptionId());
            }

            // resource group
            if (rdoUseExistResGrp.isSelected()) {
                // existing RG
                webAppOnLinuxDeployConfiguration.setCreatingNewResourceGroup(false);
                ResourceGroup selectedRg = (ResourceGroup) comboResourceGroup.getSelectedItem();
                if (selectedRg != null) {
                    webAppOnLinuxDeployConfiguration.setResourceGroupName(selectedRg.name());
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
                Location selectedLocation = (Location) cbLocation.getSelectedItem();
                if (selectedLocation != null) {
                    webAppOnLinuxDeployConfiguration.setLocationName(selectedLocation.name());
                } else {
                    webAppOnLinuxDeployConfiguration.setLocationName(null);
                }

                PricingTier selectedPricingTier = (PricingTier) cbPricing.getSelectedItem();
                if (selectedPricingTier != null) {
                    webAppOnLinuxDeployConfiguration.setPricingSkuTier(selectedPricingTier.toSkuDescription().tier());
                    webAppOnLinuxDeployConfiguration.setPricingSkuSize(selectedPricingTier.toSkuDescription().size());
                } else {
                    webAppOnLinuxDeployConfiguration.setPricingSkuTier(null);
                    webAppOnLinuxDeployConfiguration.setPricingSkuSize(null);
                }
            } else if (rdoUseExistAppServicePlan.isSelected()) {
                webAppOnLinuxDeployConfiguration.setCreatingNewAppServicePlan(false);
                AppServicePlan selectedAsp = (AppServicePlan) cbExistAppServicePlan.getSelectedItem();
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

        PrivateRegistryImageSetting acrInfo = conf.getPrivateRegistryImageSetting();
        acrInfo.setPassword(loadPassword(acrInfo.getServerUrl(), acrInfo.getUsername()));
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
        webAppOnLinuxDeployPresenter.onLoadSubscriptionList(); // including related RG & Location
        webAppOnLinuxDeployPresenter.onLoadPricingTierList();

        boolean creatingRg = conf.isCreatingNewResourceGroup();
        rdoCreateResGrp.setSelected(creatingRg);
        rdoUseExistResGrp.setSelected(!creatingRg);
        updateResourceGroupEnabled();
        if (creatingRg) {
            txtNewResGrp.setText(conf.getResourceGroupName());
        }

        boolean creatingAsp = conf.isCreatingNewAppServicePlan();
        rdoCreateAppServicePlan.setSelected(creatingAsp);
        rdoUseExistAppServicePlan.setSelected(!creatingAsp);
        updateAppServicePlanEnabled();
        if (creatingAsp) {
            txtCreateAppServicePlan.setText(conf.getAppServicePlanName());
        }

        // active panel
        boolean creatingApp = conf.isCreatingNewWebAppOnLinux();
        if (creatingApp) {
            textAppName.setText(conf.getAppName());
        }
        rdoCreateNew.setSelected(creatingApp);
        rdoUseExist.setSelected(!creatingApp);
        updatePanelVisibility();

        // default value for new resources
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());
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
        DefaultTableModel tableModel = new DefaultTableModel() {
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
            int index = webAppTable.getSelectedRow();
            if (cachedWebAppList != null && index >= 0 && index < cachedWebAppList.size()) {
                textSelectedAppName.setText(cachedWebAppList.get(webAppTable.getSelectedRow()).getResource().name());
            }
        });
        btnRefresh = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                btnRefresh.setEnabled(false);
                webAppTable.getEmptyText().setText(TABLE_LOADING_MESSAGE);
                DefaultTableModel model = (DefaultTableModel) webAppTable.getModel();
                model.getDataVector().clear();
                model.fireTableDataChanged();
                textSelectedAppName.setText("");
                webAppOnLinuxDeployPresenter.onRefreshList();
            }
        };
        ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(webAppTable)
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
    public void renderWebAppOnLinuxList(List<ResourceEx<WebApp>> webAppOnLinuxList) {
        btnRefresh.setEnabled(true);
        webAppTable.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
        List<ResourceEx<WebApp>> sortedList = webAppOnLinuxList.stream()
                .sorted((a, b) -> a.getSubscriptionId().compareToIgnoreCase(b.getSubscriptionId()))
                .collect(Collectors.toList());
        cachedWebAppList = sortedList;
        if (cachedWebAppList.size() > 0) {
            DefaultTableModel model = (DefaultTableModel) webAppTable.getModel();
            model.getDataVector().clear();
            for (ResourceEx<WebApp> resource : sortedList) {
                WebApp app = resource.getResource();
                model.addRow(new String[]{
                        app.name(),
                        app.resourceGroupName()
                });
            }
        }

        // select active web app
        for (int index = 0; index < cachedWebAppList.size(); index++) {
            if (Comparing.equal(cachedWebAppList.get(index).getResource().id(), defaultWebAppId)) {
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
                if (Comparing.equal(item.subscriptionId(), defaultSubscriptionId)) {
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
                if (Comparing.equal(item.name(), defaultResourceGroup)) {
                    comboResourceGroup.setSelectedItem(item);
                    // defaultResourceGroup = null;
                }
            });
        }
    }

    @Override
    public void renderLocationList(List<Location> locationList) {
        cbLocation.removeAllItems();
        if (locationList != null && locationList.size() > 0) {
            locationList.forEach((item) -> {
                cbLocation.addItem(item);
                if (Comparing.equal(item.name(), defaultLocationName)) {
                    cbLocation.setSelectedItem(item);
                    // defaultLocationName = null;
                }
            });
        }
    }

    @Override
    public void renderAppServicePlanList(List<AppServicePlan> appServicePlans) {
        cbExistAppServicePlan.removeAllItems();
        lblLocation.setText(NOT_APPLICABLE);
        lblPricing.setText(NOT_APPLICABLE);
        if (appServicePlans != null && appServicePlans.size() > 0) {
            appServicePlans.forEach((item) -> {
                cbExistAppServicePlan.addItem(item);
                if (Comparing.equal(item.id(), defaultAppServicePlanId)) {
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
                if (Comparing.equal(item.toString(), defaultPricingTier)) {
                    cbPricing.setSelectedItem(item);
                }
            });
        }
    }

    /**
     * Let the presenter release the view. Will be called by:
     * {@link com.microsoft.intellij.runner.container.webapponlinux.WebAppOnLinuxDeploySettingsEditor#disposeEditor()}.
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

    private void $$$setupUI$$$(){}
}
