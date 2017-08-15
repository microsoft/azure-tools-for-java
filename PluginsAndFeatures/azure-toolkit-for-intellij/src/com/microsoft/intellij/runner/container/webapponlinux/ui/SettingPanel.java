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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.runner.container.webapponlinux.WebAppOnLinuxDeployConfiguration;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.awt.event.ItemEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class SettingPanel implements WebAppOnLinuxDeployView {
    private static final String NOT_APPLICABLE = "N/A";
    private static final String TABLE_HEAD_WEB_APP_NAME = "Name";
    private static final String TABLE_HEAD_RESOURCE_GROUP = "Resource Group";
    private static final String TABLE_LOADING_MESSAGE = "Loading ... ";
    private static final String TABLE_EMPTY_MESSAGE = "No available Web App on Linux.";
    private static final String APP_NAME_PREFIX = "webapp-linux";
    private static final String RESOURCE_GROUP_NAME_PREFIX = "rg-web-linux";
    private static final String APP_SERVICE_PLAN_NAME_PREFIX = "appsp-linux-";

    private final WebAppOnLinuxDeployPresenter<SettingPanel> webAppOnLinuxDeployPresenter;
    private final Project project;
    private JTextField textServerUrl;
    private JTextField textUsername;
    private JPasswordField passwordField;
    private JTextField textAppName;
    private JComboBox comboSubscription;
    private JComboBox comboResourceGroup;
    private JTextField textImageTag;
    private JTextField textStartupFile;
    private JPanel pnlUpdate;
    private JPanel rootPanel;
    private JPanel pnlWebAppOnLinuxTable;
    private JRadioButton rdoUseExist;
    private JRadioButton rdoCreateNew;
    private JPanel pnlCreate;
    private JComboBox cbLocation;
    private JComboBox cbPricing;
    private JRadioButton rdoCreateResGrp;
    private JTextField txtNewResGrp;
    private JRadioButton rdoUseExistResGrp;
    private JRadioButton rdoCreateAppServicePlan;
    private JTextField txtCreateAppServicePlan;
    private JRadioButton rdoUseExistAppServicePlan;
    private JComboBox cbExistAppServicePlan;
    private JLabel lblLocation;
    private JLabel lblPricing;
    private JPanel pnlAcr;
    private JPanel pnlWebAppInfo;
    private JBTable webAppTable;
    private AnActionButton btnRefresh;
    private List<ResourceEx<SiteInner>> cachedWebAppList;
    private String defaultWebAppId;
    private String defaultLocationName;
    private String defaultPricingTier;
    private String defaultResourceGroup;
    private String defaultSubscriptionId;
    private String defaultAppServicePlanId;
    private JTextField textSelectedAppName; // invisible, used to trigger validation on tableRowSelection
    private JComboBox<Artifact> cbArtifact;
    private JLabel lblArtifact;
    private Artifact lastSelectedArtifact;
    private boolean isCbArtifactInited;

    private boolean telemetrySent;

    /**
     * Constructor.
     */
    public SettingPanel(Project project) {
        webAppOnLinuxDeployPresenter = new WebAppOnLinuxDeployPresenter<>();
        webAppOnLinuxDeployPresenter.onAttachView(this);
        this.project = project;

        pnlAcr.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.GRAY),
                "Azure Container Registry"));
        pnlWebAppInfo.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.GRAY),
                "Web App on Linux"));

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

        // Artifact to build
        isCbArtifactInited = false;
        cbArtifact.addActionListener(e -> {
            final Artifact selectedArtifact = (Artifact) cbArtifact.getSelectedItem();
            if (!Comparing.equal(lastSelectedArtifact, selectedArtifact)) {
                if (isCbArtifactInited) {
                    if (lastSelectedArtifact != null) {
                        BuildArtifactsBeforeRunTaskProvider
                                .setBuildArtifactBeforeRunOption(rootPanel, project, lastSelectedArtifact, false);
                    }
                    if (selectedArtifact != null) {
                        BuildArtifactsBeforeRunTaskProvider
                                .setBuildArtifactBeforeRunOption(rootPanel, project, selectedArtifact, true);
                    }
                }
                lastSelectedArtifact = selectedArtifact;

            }
        });

        cbArtifact.setRenderer(new ListCellRendererWrapper<Artifact>() {
            @Override
            public void customize(JList jList, Artifact artifact, int i, boolean b, boolean b1) {
                if (artifact != null) {
                    setIcon(artifact.getArtifactType().getIcon());
                    setText(artifact.getName());
                }
            }
        });

        telemetrySent = false;
    }

    // TODO: refactor later
    private void sendTelemetry(String subId) {
        if (telemetrySent) {
            return;
        }
        Observable.fromCallable(() ->{
            Map<String, String> map = new HashMap<String, String>();
            map.put("SubscriptionId", subId);
            AppInsightsClient.createByType(AppInsightsClient.EventType.Dialog
                    ,"Run On Web App (Linux)"
                    ,"Open"
                    ,map);
            return true;
        }).subscribeOn(Schedulers.io()).subscribe(
               (res) -> {
                   telemetrySent = true;
               },
               (err) -> {
                   telemetrySent = true;
               }
        );
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

    public JPanel getRootPanel() {
        return rootPanel;
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
    public void apply(WebAppOnLinuxDeployConfiguration webAppOnLinuxDeployConfiguration) {
        // set ACR info
        webAppOnLinuxDeployConfiguration.setPrivateRegistryImageSetting(new PrivateRegistryImageSetting(
                textServerUrl.getText(),
                textUsername.getText(),
                String.valueOf(passwordField.getPassword()),
                textImageTag.getText(),
                textStartupFile.getText()
        ));

        // set target
        if (lastSelectedArtifact != null) {
            webAppOnLinuxDeployConfiguration.setTargetPath(lastSelectedArtifact.getOutputFilePath());
            Path p = Paths.get(webAppOnLinuxDeployConfiguration.getTargetPath());
            if (null != p) {
                webAppOnLinuxDeployConfiguration.setTargetName(p.getFileName().toString());
            } else {
                webAppOnLinuxDeployConfiguration.setTargetName(lastSelectedArtifact.getName() + "." + MavenConstants.TYPE_WAR);
            }
        } else {
            MavenProject mavenProject = MavenRunTaskUtil.getMavenProject(project);
            if (mavenProject != null) {
                // TODO: move to a util
                String targetPath = new File(mavenProject.getBuildDirectory()).getPath()
                        + File.separator + mavenProject.getFinalName() + "." + mavenProject.getPackaging();
                String targetName = mavenProject.getFinalName() + "." + mavenProject.getPackaging();
                webAppOnLinuxDeployConfiguration.setTargetPath(targetPath);
                webAppOnLinuxDeployConfiguration.setTargetName(targetName);
            }
        }

        // set web app info
        if (rdoUseExist.isSelected()) {
            // existing web app
            webAppOnLinuxDeployConfiguration.setCreatingNewWebAppOnLinux(false);
            ResourceEx<SiteInner> selectedWebApp = null;
            int index = webAppTable.getSelectedRow();
            if (cachedWebAppList != null && index >= 0 && index < cachedWebAppList.size()) {
                selectedWebApp = cachedWebAppList.get(webAppTable.getSelectedRow());
            }
            if (selectedWebApp != null) {
                webAppOnLinuxDeployConfiguration.setWebAppId(selectedWebApp.getResource().id());
                webAppOnLinuxDeployConfiguration.setAppName(selectedWebApp.getResource().name());
                webAppOnLinuxDeployConfiguration.setSubscriptionId(selectedWebApp.getSubscriptionId());
                webAppOnLinuxDeployConfiguration.setResourceGroupName(selectedWebApp.getResource().resourceGroup());
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
                    webAppOnLinuxDeployConfiguration.setLocationName(selectedLocation.region().name());
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

    private void setupArtifactCombo(List<Artifact> artifacts, String targetPath) {
        isCbArtifactInited = false;
        cbArtifact.removeAllItems();
        if (null != artifacts) {
            for (Artifact artifact : artifacts) {
                cbArtifact.addItem(artifact);
                if (null != targetPath && Comparing.equal(artifact.getOutputFilePath(), targetPath)) {
                    cbArtifact.setSelectedItem(artifact);
                }
            }
        }
        cbArtifact.setVisible(true);
        lblArtifact.setVisible(true);
        isCbArtifactInited = true;
    }

    /**
     * Function triggered in constructing the panel.
     *
     * @param conf configuration instance
     */
    public void reset(WebAppOnLinuxDeployConfiguration conf) {
        if (!MavenRunTaskUtil.isMavenProject(project)) {
            List<Artifact> artifacts = MavenRunTaskUtil.collectProjectArtifact(project);
            setupArtifactCombo(artifacts, conf.getTargetPath());
        }

        PrivateRegistryImageSetting acrInfo = conf.getPrivateRegistryImageSetting();
        textServerUrl.setText(acrInfo.getServerUrl());
        textUsername.setText(acrInfo.getUsername());
        passwordField.setText(acrInfo.getPassword());
        textImageTag.setText(acrInfo.getImageNameWithTag());
        textStartupFile.setText(acrInfo.getStartupFile());

        // cache for table/combo selection
        defaultSubscriptionId = conf.getSubscriptionId();
        defaultWebAppId = conf.getWebAppId();
        defaultLocationName = conf.getLocationName();
        defaultPricingTier = new PricingTier(conf.getPricingSkuTier(), conf.getPricingSkuSize()).toString();
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
        if(Utils.isEmptyString(textAppName.getText())) {
            textAppName.setText(String.format("%s-%s", APP_NAME_PREFIX, date));
        }
        if(Utils.isEmptyString(txtNewResGrp.getText())) {
            txtNewResGrp.setText(String.format("%s-%s", RESOURCE_GROUP_NAME_PREFIX, date));
        }
        if(Utils.isEmptyString(txtCreateAppServicePlan.getText())) {
            txtCreateAppServicePlan.setText(String.format("%s-%s", APP_SERVICE_PLAN_NAME_PREFIX, date));
        }

        sendTelemetry(conf.getSubscriptionId());

    }

    private void loadWebAppList() {
        btnRefresh.setEnabled(false);
        webAppTable.getEmptyText().setText(TABLE_LOADING_MESSAGE);
        webAppOnLinuxDeployPresenter.onLoadAppList();
    }

    private void createUIComponents() {
        System.out.println(" createUIComponents");
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
    public void renderWebAppOnLinuxList(List<ResourceEx<SiteInner>> webAppOnLinuxList) {
        btnRefresh.setEnabled(true);
        webAppTable.getEmptyText().setText(TABLE_EMPTY_MESSAGE);
        List<ResourceEx<SiteInner>> sortedList = webAppOnLinuxList.stream()
                .sorted((a, b) -> a.getSubscriptionId().compareToIgnoreCase(b.getSubscriptionId()))
                .collect(Collectors.toList());
        cachedWebAppList = sortedList;
        if (cachedWebAppList.size() > 0) {
            DefaultTableModel model = (DefaultTableModel) webAppTable.getModel();
            model.getDataVector().clear();
            for (ResourceEx<SiteInner> resource : sortedList) {
                SiteInner app = resource.getResource();
                model.addRow(new String[]{
                        app.name(),
                        app.resourceGroup()
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
                if (Comparing.equal(item.region().name(), defaultLocationName)) {
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
            appServicePlans.stream().filter(item -> Comparing.equal(item.operatingSystem(), OperatingSystem.LINUX))
                    .forEach((item) -> {
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
                    defaultPricingTier = null;
                }
            });
        }

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
}
