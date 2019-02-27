package com.microsoft.intellij.runner.webapp.webappconfig.slimui.creation;


import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.JdkModel;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class WebAppCreationDialog extends JDialog implements WebAppCreationMvpView {

    private static final String DIALOG_TITLE = "Create WebApp";
    private static final String NOT_APPLICABLE = "N/A";
    private static final String WARNING_MESSAGE = "<html><font size=\"3\" color=\"red\">%s</font></html>";

    public static final RuntimeStack DEFAULT_LINUX_RUNTIME = RuntimeStack.JAVA_8_JRE8;
    public static final JdkModel DEFAULT_WINDOWS_JAVAVERSION = JdkModel.JAVA_ZULU_1_8_0_102;
    public static final WebAppUtils.WebContainerMod DEFAULT_WINDOWS_CONTAINER =
        WebAppUtils.WebContainerMod.Newest_Tomcat_85;
    public static final PricingTier DEFAULT_PRICINGTIER = new PricingTier("Premium", "P1V2");
    public static final Region DEFAULT_REGION = Region.EUROPE_WEST;

    private WebAppCreationViewPresenter presenter = null;

    private JPanel contentPanel;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel pnlCreate;
    private JTextField txtWebAppName;
    private JComboBox cbSubscription;
    private JRadioButton rdoUseExistResGrp;
    private JComboBox cbExistResGrp;
    private JRadioButton rdoCreateResGrp;
    private JTextField txtNewResGrp;
    private JRadioButton rdoUseExistAppServicePlan;
    private JComboBox cbExistAppServicePlan;
    private JLabel lblLocation;
    private JLabel lblPricing;
    private JRadioButton rdoCreateAppServicePlan;
    private JTextField txtCreateAppServicePlan;
    private JComboBox cbLocation;
    private JComboBox cbPricing;
    private JLabel lblJavaVersion;
    private JComboBox cbJdkVersion;
    private JLabel lblWebContainer;
    private JComboBox cbWebContainer;
    private JRadioButton rdoLinuxOS;
    private JRadioButton rdoWindowsOS;
    private JLabel lblOS;
    private JPanel pnlNewAppServicePlan;
    private JPanel pnlExistingAppServicePlan;
    private JLabel lblMessage;
    private JLabel lblRuntimeStack;
    private JComboBox cbRuntimeStack;
    private JPanel lblPanelRoot;

    private WebAppConfiguration webAppConfiguration;
    private WebApp result = null;

    public WebAppCreationDialog(WebAppConfiguration configuration) {
        setContentPane(contentPanel);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        this.setTitle(DIALOG_TITLE);
        this.webAppConfiguration = configuration;
        this.presenter = new WebAppCreationViewPresenter<>();
        this.presenter.onAttachView(this);

        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());
        cbSubscription.addActionListener(e -> selectSubscription());
        cbExistAppServicePlan.addActionListener(e -> selectAppServicePlan());

        rdoCreateAppServicePlan.addActionListener(e -> toggleAppServicePlan(true));
        rdoUseExistAppServicePlan.addActionListener(e -> toggleAppServicePlan(false));

        rdoCreateResGrp.addActionListener(e -> toggleResourceGroup(true));
        rdoUseExistResGrp.addActionListener(e -> toggleResourceGroup(false));

        rdoLinuxOS.addActionListener(e -> toggleOS(false));
        rdoWindowsOS.addActionListener(e -> toggleOS(true));

        final ButtonGroup resourceGroupButtonGroup = new ButtonGroup();
        resourceGroupButtonGroup.add(rdoUseExistResGrp);
        resourceGroupButtonGroup.add(rdoCreateResGrp);

        final ButtonGroup appServicePlanButtonGroup = new ButtonGroup();
        appServicePlanButtonGroup.add(rdoUseExistAppServicePlan);
        appServicePlanButtonGroup.add(rdoCreateAppServicePlan);

        final ButtonGroup osButtonGroup = new ButtonGroup();
        osButtonGroup.add(rdoLinuxOS);
        osButtonGroup.add(rdoWindowsOS);

        cbSubscription.setRenderer(new ListCellRendererWrapper<Subscription>() {
            @Override
            public void customize(JList list, Subscription subscription, int
                index, boolean isSelected, boolean cellHasFocus) {
                if (subscription != null) {
                    setText(subscription.displayName());
                }
            }
        });

        cbExistResGrp.setRenderer(new ListCellRendererWrapper<ResourceGroup>() {
            @Override
            public void customize(JList list, ResourceGroup resourceGroup, int
                index, boolean isSelected, boolean cellHasFocus) {
                if (resourceGroup != null) {
                    setText(resourceGroup.name());
                }
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

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        addValidationListener(contentPanel, e -> validateConfiguration());
        init();
    }

    private void addValidationListener(Container parent, ActionListener actionListener) {
        for (Component component : parent.getComponents()) {
            if (component instanceof AbstractButton) {
                ((AbstractButton) component).addActionListener(actionListener);
            } else if (component instanceof JComboBox) {
                ((JComboBox) component).addActionListener(actionListener);
            } else if (component instanceof JTextField) {
                ((JTextField) component).getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        validateConfiguration();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        validateConfiguration();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        validateConfiguration();
                    }
                });
            } else if (component instanceof Container) {
                addValidationListener((Container) component, actionListener);
            }
        }
    }

    private void toggleOS(boolean isWindows) {
        lblJavaVersion.setVisible(isWindows);
        cbJdkVersion.setVisible(isWindows);
        lblWebContainer.setVisible(isWindows);
        cbWebContainer.setVisible(isWindows);
        lblRuntimeStack.setVisible(!isWindows);
        cbRuntimeStack.setVisible(!isWindows);
        // Filter App Service Plan
        Subscription subscription = (Subscription) cbSubscription.getSelectedItem();
        if (subscription != null) {
            presenter.onLoadAppServicePlan(subscription.subscriptionId());
        }
        pack();
    }

    private void init() {
        final String projectName = webAppConfiguration.getProject().getName();
        final DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        final String date = df.format(new Date());
        final String defaultWebAppName = String.format("%s-%s", projectName, date);
        final String defaultNewResourceGroup = String.format("rg-webapp-%s", projectName);
        final String defaultNewServicePlanName = String.format("appsp-%s", defaultWebAppName);

        txtWebAppName.setText(defaultWebAppName);
        txtNewResGrp.setText(defaultNewResourceGroup);
        txtCreateAppServicePlan.setText(defaultNewServicePlanName);

        presenter.onLoadWebContainer();
        presenter.onLoadSubscription();
        presenter.onLoadPricingTier();
        presenter.onLoadJavaVersions();
        presenter.onLoadLinuxRuntimes();
    }

    private void selectSubscription() {
        Subscription subscription = (Subscription) cbSubscription.getSelectedItem();
        presenter.onLoadLocation(subscription.subscriptionId());
        presenter.onLoadResourceGroups(subscription.subscriptionId());
        presenter.onLoadAppServicePlan(subscription.subscriptionId());
    }

    private void toggleResourceGroup(boolean isCreateNew) {
        cbExistResGrp.setVisible(!isCreateNew);
        txtNewResGrp.setVisible(isCreateNew);
        pack();
    }

    private void toggleAppServicePlan(boolean isCreateNew) {
        pnlNewAppServicePlan.setVisible(isCreateNew);
        pnlExistingAppServicePlan.setVisible(!isCreateNew);
        pack();
    }

    private void selectAppServicePlan() {
        AppServicePlan appServicePlan = (AppServicePlan) cbExistAppServicePlan.getSelectedItem();
        if (appServicePlan == null) {
            lblLocation.setText(NOT_APPLICABLE);
            lblPricing.setText(NOT_APPLICABLE);
        } else {
            lblLocation.setText(appServicePlan.regionName());
            lblPricing.setText(appServicePlan.pricingTier().toString());
        }
    }

    private void onOK() {
        createWebApp();
    }

    public WebApp getCreatedWebApp() {
        return this.result;
    }

    private void updateConfiguration() {
        webAppConfiguration.setWebAppName(txtWebAppName.getText());
        webAppConfiguration.setSubscriptionId(cbSubscription.getSelectedItem() == null ? "" :
            ((Subscription) cbSubscription.getSelectedItem()).subscriptionId());
        // resource group
        if (rdoCreateResGrp.isSelected()) {
            webAppConfiguration.setCreatingResGrp(true);
            webAppConfiguration.setResourceGroup(txtNewResGrp.getText());
        } else {
            webAppConfiguration.setCreatingResGrp(false);
            webAppConfiguration.setResourceGroup(cbExistResGrp.getSelectedItem() == null ? "" :
                ((ResourceGroup) cbExistResGrp.getSelectedItem()).name());
        }
        // app service plan
        if (rdoCreateAppServicePlan.isSelected()) {
            webAppConfiguration.setCreatingAppServicePlan(true);
            webAppConfiguration.setAppServicePlanName(txtCreateAppServicePlan.getText());
            webAppConfiguration.setRegion(cbLocation.getSelectedItem() == null ? DEFAULT_REGION.name() :
                ((Location) cbLocation.getSelectedItem()).region().name());
            webAppConfiguration.setPricing(cbPricing.getSelectedItem() == null ? DEFAULT_PRICINGTIER.toString() :
                cbPricing.getSelectedItem().toString());
        } else {
            webAppConfiguration.setCreatingAppServicePlan(false);
            webAppConfiguration.setAppServicePlanId(cbExistAppServicePlan.getSelectedItem() == null ? null :
                ((AppServicePlan) cbExistAppServicePlan.getSelectedItem()).id());
        }
        // runtime
        if (rdoLinuxOS.isSelected()) {
            webAppConfiguration.setOS(OperatingSystem.LINUX);
            RuntimeStack linuxRuntime = cbRuntimeStack.getSelectedItem() == null ? null :
                (RuntimeStack) cbRuntimeStack.getSelectedItem();
            if (linuxRuntime != null) {
                webAppConfiguration.setStack(linuxRuntime.stack());
                webAppConfiguration.setVersion(linuxRuntime.version());
            }
        }
        if (rdoWindowsOS.isSelected()) {
            webAppConfiguration.setOS(OperatingSystem.WINDOWS);
            webAppConfiguration.setJdkVersion(cbJdkVersion.getSelectedItem() == null ? null :
                ((JdkModel) cbJdkVersion.getSelectedItem()).getJavaVersion());
            webAppConfiguration.setWebContainer(cbWebContainer.getSelectedItem() == null ? null :
                ((WebAppUtils.WebContainerMod) cbWebContainer.getSelectedItem()).getValue());
        }
        webAppConfiguration.setCreatingNew(true);
    }

    private void validateConfiguration() {
        updateConfiguration();
        try {
            webAppConfiguration.validate();
            lblMessage.setText("");
            buttonOK.setEnabled(true);
        } catch (ConfigurationException e) {
            lblMessage.setText(String.format(WARNING_MESSAGE, e.getMessage()));
            buttonOK.setEnabled(false);
        }
    }

    private void onCancel() {
        dispose();
    }

    private void createWebApp() {
        updateConfiguration();
        ProgressManager.getInstance().run(new Task.Modal(null, "Creating New WebApp...", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);
                    result = AzureWebAppMvpModel.getInstance().createWebApp(webAppConfiguration.getModel());
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Create WebApp Failed : " + ex.getMessage(), "Create WebApp " +
                            "Failed",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    @Override
    public void fillSubscription(@NotNull List<Subscription> subscriptions) {
        fillCombobox(this, cbSubscription, subscriptions, null);
    }

    @Override
    public void fillResourceGroup(@NotNull List<ResourceGroup> resourceGroups) {
        fillCombobox(this, cbExistResGrp, resourceGroups, null);
    }

    @Override
    public void fillAppServicePlan(@NotNull List<AppServicePlan> appServicePlans) {
        cbExistAppServicePlan.removeAllItems();
        appServicePlans.stream()
            .filter(item -> Comparing.equal(item.operatingSystem(), webAppConfiguration.getOS()))
            .sorted(Comparator.comparing(AppServicePlan::name))
            .forEach((plan) -> {
                cbExistAppServicePlan.addItem(plan);
            });
        selectAppServicePlan();
        pack();
    }

    @Override
    public void fillLocation(@NotNull List<Location> locations) {
        cbLocation.removeAllItems();
        locations.stream()
            .sorted(Comparator.comparing(Location::displayName))
            .forEach((location) -> {
                cbLocation.addItem(location);
                if (Comparing.equal(location.name(), DEFAULT_REGION.name())) {
                    cbLocation.setSelectedItem(location);
                }
            });
        pack();
    }

    @Override
    public void fillPricingTier(@NotNull List<PricingTier> prices) {
        fillCombobox(this, cbPricing, prices, DEFAULT_PRICINGTIER);
    }

    @Override
    public void fillWebContainer(@NotNull List<WebAppUtils.WebContainerMod> webContainers) {
        fillCombobox(this, cbWebContainer, webContainers, DEFAULT_WINDOWS_CONTAINER);
    }

    @Override
    public void fillJdkVersion(@NotNull List<JdkModel> jdks) {
        fillCombobox(this, cbJdkVersion, jdks, DEFAULT_WINDOWS_JAVAVERSION);
    }

    @Override
    public void fillLinuxRuntime(@NotNull List<RuntimeStack> linuxRuntimes) {
        fillCombobox(this, cbRuntimeStack, linuxRuntimes, DEFAULT_LINUX_RUNTIME);
    }

    public static <T> void fillCombobox(Window window, JComboBox<T> comboBox, List<T> values, T defaultValue) {
        comboBox.removeAllItems();
        values.forEach(value -> comboBox.addItem(value));
        if (defaultValue != null && values.contains(defaultValue)) {
            comboBox.setSelectedItem(defaultValue);
        }
        window.pack();
    }
}
