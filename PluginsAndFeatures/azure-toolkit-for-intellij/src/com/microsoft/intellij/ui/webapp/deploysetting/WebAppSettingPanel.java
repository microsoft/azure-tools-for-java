/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.webapp.deploysetting;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.utils.AzulZuluModel;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppSettingModel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WebAppSettingPanel implements WebAppDeployMvpView {

    // presenter
    private final WebAppDeployViewPresenter<WebAppSettingPanel> webAppDeployViewPresenter;

    // cache variable
    private ResourceEx<WebApp> selectedWebApp = null;
    private final Project project;
    private List<ResourceEx<WebApp>> cachedWebAppList = null;

    private String lastSelectedSid;
    private String lastSelectedResGrp;

    // const
    private static final String URL_PREFIX = "https://";
    private static final String NOT_APPLICABLE = "N/A";

    //widgets
    private JPanel pnlRoot;
    private JPanel pnlExist;
    private JPanel pnlCreate;
    private JPanel pnlWebAppTable;
    private JCheckBox chkToRoot;
    private JRadioButton rdoUseExist;
    private JRadioButton rdoCreateNew;
    private JRadioButton rdoCreateAppServicePlan;
    private JRadioButton rdoUseExistAppServicePlan;
    private JRadioButton rdoCreateResGrp;
    private JRadioButton rdoUseExistResGrp;
    private JRadioButton rdoDefaultJdk;
    private JRadioButton rdoThirdPartyJdk;
    private JRadioButton rdoDownloadOwnJdk;
    private JTextField txtWebAppName;
    private JTextField txtCreateAppServicePlan;
    private JTextField txtNewResGrp;
    private JTextField txtJdkUrl;
    private JTextField txtAccountKey;
    private JComboBox<Subscription> cbSubscription;
    private JComboBox<WebAppUtils.WebContainerMod> cbWebContainer;
    private JComboBox<Location> cbLocation;
    private JComboBox<PricingTier> cbPricing;
    private JComboBox<AppServicePlan> cbExistAppServicePlan;
    private JComboBox<ResourceGroup> cbExistResGrp;
    private JComboBox<AzulZuluModel> cbThirdPartyJdk;
    private HyperlinkLabel lblJdkLicense;
    private JLabel lblLocation;
    private JLabel lblPricing;
    private JLabel lblDefaultJdk;
    private JLabel lblKeyExplanation;
    private JTable table;

    /**
     * The setting panel for web app deployment run configuration
     */
    public WebAppSettingPanel(Project project) {
        this.project = project;
        this.webAppDeployViewPresenter = new WebAppDeployViewPresenter<>();
        this.webAppDeployViewPresenter.onAttachView(this);

        final ButtonGroup btnGrpForDeploy = new ButtonGroup();
        btnGrpForDeploy.add(rdoUseExist);
        btnGrpForDeploy.add(rdoCreateNew);
        rdoUseExist.addActionListener(e -> toggleDeployPanel(true /*isUsingExisting*/));
        rdoCreateNew.addActionListener(e -> toggleDeployPanel(false /*isUsingExisting*/));
        toggleDeployPanel(true /*showUsingExisting*/);

        final ButtonGroup btnGrpForResGrp = new ButtonGroup();
        btnGrpForResGrp.add(rdoUseExistResGrp);
        btnGrpForResGrp.add(rdoCreateResGrp);
        rdoCreateResGrp.addActionListener(e -> toggleResGrpPanel(true /*isCreatingNew*/));
        rdoUseExistResGrp.addActionListener(e -> toggleResGrpPanel(false /*isCreatingNew*/));

        final ButtonGroup btnGrpForAppServicePlan = new ButtonGroup();
        btnGrpForAppServicePlan.add(rdoUseExistAppServicePlan);
        btnGrpForAppServicePlan.add(rdoCreateAppServicePlan);
        rdoUseExistAppServicePlan.addActionListener(e -> toggleAppServicePlanPanel(false /*isCreatingNew*/));
        rdoCreateAppServicePlan.addActionListener(e -> toggleAppServicePlanPanel(true /*isCreatingNew*/));

        final ButtonGroup btnGrpForJdk = new ButtonGroup();
        btnGrpForJdk.add(rdoDefaultJdk);
        btnGrpForJdk.add(rdoThirdPartyJdk);
        btnGrpForJdk.add(rdoDownloadOwnJdk);
        rdoDefaultJdk.addActionListener(e -> toggleJdkPanel(JdkChoice.DEFAULT));
        rdoThirdPartyJdk.addActionListener(e -> toggleJdkPanel(JdkChoice.THIRD_PARTY));
        rdoDownloadOwnJdk.addActionListener(e -> toggleJdkPanel(JdkChoice.URL));

        cbExistResGrp.setRenderer(new ListCellRendererWrapper<ResourceGroup>() {
            @Override
            public void customize(JList list, ResourceGroup resourceGroup, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (resourceGroup != null) {
                    setText(resourceGroup.name());
                }
            }
        });

        cbExistResGrp.addActionListener(e -> {
            ResourceGroup resGrp = (ResourceGroup) cbExistResGrp.getSelectedItem();
            if (resGrp == null) {
                return;
            }
            String selectedGrp = resGrp.name();
            if (!Comparing.equal(lastSelectedResGrp, selectedGrp)) {
                cbExistAppServicePlan.removeAllItems();
                lblLocation.setText(NOT_APPLICABLE);
                lblPricing.setText(NOT_APPLICABLE);
                webAppDeployViewPresenter.onLoadAppServicePlan(lastSelectedSid, selectedGrp);
                lastSelectedResGrp = selectedGrp;
            }
        });

        cbSubscription.setRenderer(new ListCellRendererWrapper<Subscription>() {
            @Override
            public void customize(JList list, Subscription subscription, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (subscription != null) {
                    setText(subscription.displayName());
                }
            }
        });

        cbSubscription.addActionListener(e -> {
            Subscription subscription = (Subscription) cbSubscription.getSelectedItem();
            if (subscription == null) {
                return;
            }
            String selectedSid = subscription.subscriptionId();
            if (!Comparing.equal(lastSelectedSid, selectedSid)) {
                cbExistResGrp.removeAllItems();
                cbLocation.removeAllItems();
                webAppDeployViewPresenter.onLoadResourceGroups(selectedSid);
                webAppDeployViewPresenter.onLoadLocation(selectedSid);
                lastSelectedSid = selectedSid;
            }
        });

        cbLocation.setRenderer(new ListCellRendererWrapper<Location>() {
            @Override
            public void customize(JList list, Location location, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (location != null) {
                    setText(location.displayName());
                }
            }
        });

        cbExistAppServicePlan.setRenderer(new ListCellRendererWrapper<AppServicePlan>() {
            @Override
            public void customize(JList list, AppServicePlan appServicePlan, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (appServicePlan != null) {
                    setText(appServicePlan.name());
                }
            }
        });

        cbExistAppServicePlan.addActionListener(e -> {
            AppServicePlan plan = (AppServicePlan) cbExistAppServicePlan.getSelectedItem();
            if (plan != null) {
                lblLocation.setText(plan.regionName());
                lblPricing.setText(plan.pricingTier().toString());
            }
        });

        cbThirdPartyJdk.setRenderer(new ListCellRendererWrapper<AzulZuluModel>() {
            @Override
            public void customize(JList list, AzulZuluModel azulZuluModel, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (azulZuluModel != null) {
                    setText(azulZuluModel.getName());
                }
            }
        });

        fillWebContainers();
        fillThirdParty();
        fillPricingTier();

        lblJdkLicense.setHyperlinkText("License");
        lblJdkLicense.setHyperlinkTarget(AzulZuluModel.getLicenseUrl());
        this.webAppDeployViewPresenter.onLoadSubscription();
    }

    public JPanel getMainPanel() {
        return pnlRoot;
    }

    public void resetEditorForm(WebAppSettingModel model) {
        chkToRoot.setSelected(model.isDeployToRoot());
        this.webAppDeployViewPresenter.onLoadWebApps();
    }

    @Override
    public void renderWebAppsTable(@NotNull List<ResourceEx<WebApp>> webAppLists) {
        List<ResourceEx<WebApp>> sortedList = webAppLists.stream()
                .filter(resource -> resource.getResource().javaVersion() != JavaVersion.OFF)
                .sorted((a, b) -> a.getSubscriptionId().compareToIgnoreCase(b.getSubscriptionId()))
                .collect(Collectors.toList());
        cachedWebAppList = sortedList;
        if (sortedList.size() > 0) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.getDataVector().clear();
            for (ResourceEx<WebApp> resource: sortedList) {
                WebApp app = resource.getResource();
                model.addRow(new String[]{
                        app.name(),
                        app.javaVersion().toString(),
                        app.javaContainer() + " " + app.javaContainerVersion(),
                        app.resourceGroupName(),
                });
            }
            model.fireTableDataChanged();
        }
    }

    public String getTargetPath() {
        MavenProject mavenProject = MavenProjectsManager.getInstance(project).getRootProjects().get(0);
        return new File(mavenProject.getBuildDirectory()).getPath()
                + File.separator + mavenProject.getFinalName() + "." + mavenProject.getPackaging();
    }

    public String getTargetName() {
        MavenProject mavenProject = MavenProjectsManager.getInstance(project).getRootProjects().get(0);
        return mavenProject.getFinalName() + "." + mavenProject.getPackaging();
    }

    public String getSelectedWebAppId() {
        return selectedWebApp == null ? "" : selectedWebApp.getResource().id();
    }

    public String getSubscriptionIdOfSelectedWebApp() {
        return selectedWebApp == null ? "" : selectedWebApp.getSubscriptionId();
    }

    public String getWebAppUrl() {
        return selectedWebApp == null ? "" : URL_PREFIX + selectedWebApp.getResource().defaultHostName();
    }

    public boolean isDeployToRoot() {
        return chkToRoot.isSelected();
    }

    public boolean isCreatingNew() {
        return rdoCreateNew.isSelected();
    }

    private void toggleDeployPanel(boolean isUsingExisting) {
        pnlExist.setVisible(isUsingExisting);
        pnlCreate.setVisible(!isUsingExisting);
    }

    private void toggleResGrpPanel(boolean isCreatingNew) {
        txtNewResGrp.setEnabled(isCreatingNew);
        cbExistResGrp.setEnabled(!isCreatingNew);
    }

    private void toggleAppServicePlanPanel(boolean isCreatingNew) {
        txtCreateAppServicePlan.setEnabled(isCreatingNew);
        cbLocation.setEnabled(isCreatingNew);
        cbPricing.setEnabled(isCreatingNew);
        cbExistAppServicePlan.setEnabled(!isCreatingNew);
        lblLocation.setEnabled(!isCreatingNew);
        lblPricing.setEnabled(!isCreatingNew);
    }

    private void toggleJdkPanel(JdkChoice choice) {
        switch (choice) {
            case DEFAULT:
                lblDefaultJdk.setEnabled(true);
                cbThirdPartyJdk.setEnabled(false);
                txtJdkUrl.setEnabled(false);
                txtAccountKey.setEnabled(false);
                lblKeyExplanation.setEnabled(false);
                break;
            case THIRD_PARTY:
                lblDefaultJdk.setEnabled(false);
                cbThirdPartyJdk.setEnabled(true);
                txtJdkUrl.setEnabled(false);
                txtAccountKey.setEnabled(false);
                lblKeyExplanation.setEnabled(false);
                break;
            case URL:
                lblDefaultJdk.setEnabled(false);
                cbThirdPartyJdk.setEnabled(false);
                txtJdkUrl.setEnabled(true);
                txtAccountKey.setEnabled(true);
                lblKeyExplanation.setEnabled(true);
                break;
            default:
                break;
        }
    }

    private void resetWidget() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.getDataVector().clear();
        model.fireTableDataChanged();
    }

    private void createUIComponents() {
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.addColumn("Name");
        tableModel.addColumn("JDK");
        tableModel.addColumn("Web container");
        tableModel.addColumn("Resource group");

        table = new JBTable(tableModel);

        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(event -> {
            if (cachedWebAppList != null) {
                selectedWebApp = cachedWebAppList.get(table.getSelectedRow());
            }
        });

        AnActionButton refreshAction = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                resetWidget();
                webAppDeployViewPresenter.onRefresh();
            }
        };

        ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(table)
                .addExtraActions(refreshAction).setToolbarPosition(ActionToolbarPosition.TOP);

        pnlWebAppTable = tableToolbarDecorator.createPanel();
    }

    private void fillWebContainers() {
        DefaultComboBoxModel<WebAppUtils.WebContainerMod> cbModel = new DefaultComboBoxModel<>();
        for (WebAppUtils.WebContainerMod wc : WebAppUtils.WebContainerMod.values()) {
            cbModel.addElement(wc);
        }
        cbWebContainer.setModel(cbModel);
    }

    private void fillThirdParty() {
        DefaultComboBoxModel<AzulZuluModel> cbModel = new DefaultComboBoxModel<>();
        for (AzulZuluModel jdk : AzulZuluModel.values()) {
            if (jdk.isDeprecated()) {
                continue;
            }
            cbModel.addElement(jdk);
        }
        cbThirdPartyJdk.setModel(cbModel);
    }

    private void fillPricingTier() {
        for (Field field: PricingTier.class.getDeclaredFields()) {
            int modifier = field.getModifiers();
            if (Modifier.isPublic(modifier) && Modifier.isStatic(modifier) && Modifier.isFinal(modifier)) {
                try {
                    PricingTier pt = (PricingTier) field.get(null);
                    cbPricing.addItem(pt);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void fillSubscription(List<Subscription> subscriptions) {
        subscriptions.forEach(cbSubscription::addItem);
    }

    @Override
    public void fillResourceGroup(List<ResourceGroup> resourceGroups) {
        resourceGroups.forEach(cbExistResGrp::addItem);
    }

    @Override
    public void fillAppServicePlan(List<AppServicePlan> appServicePlans) {
        appServicePlans.forEach(cbExistAppServicePlan::addItem);
    }

    @Override
    public void fillLocation(List<Location> locations) {
        locations.stream().sorted(Comparator.comparing(Location::displayName)).forEach(cbLocation::addItem);
    }

    private enum JdkChoice {
        DEFAULT, THIRD_PARTY, URL
    }
}
