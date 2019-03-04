package com.microsoft.intellij.runner.webapp.webappconfig.slimui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.webapp.Constants;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppConfiguration;
import com.microsoft.intellij.runner.webapp.webappconfig.slimui.creation.WebAppCreationDialog;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import icons.MavenIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WebAppSlimSettingPanel extends AzureSettingPanel<WebAppConfiguration> implements WebAppDeployMvpViewSlim {

    private static final String DEPLOYMENT_SLOT = "Deployment Slot";
    private static final String DEFAULT_SLOT_NAME = "slot-%s";
    private static final String CREATE_NEW_WEBAPP = "Create New WebApp";
    private static final String REFRESHING_WEBAPP = "Refreshing...";
    private static final String DEPLOYMENT_SLOT_HOVER = "Deployment slots are live apps with their own hostnames. App" +
        " content and configurations elements can be swapped between two deployment slots, including the production " +
        "slot.";

    private boolean refreshingWebApp = false;
    private ResourceEx<WebApp> selectedWebApp = null;
    private WebAppDeployViewPresenterSlim presenter = null;

    private JPanel pnlSlotCheckBox;
    private JTextField txtNewSlotName;
    private JComboBox cbxSlotConfigurationSource;
    private JCheckBox chkDeployToSlot;
    private JLabel lblArtifact;
    private JComboBox cbArtifact;
    private JCheckBox chkToRoot;
    private JLabel lblMavenProject;
    private JComboBox cbMavenProject;
    private JPanel pnlRoot;
    private JComboBox cbxWebApp;
    private JCheckBox chkBuild;
    private JPanel pnlSlotDetails;
    private JRadioButton rbtNewSlot;
    private JRadioButton rbtExistingSlot;
    private JComboBox cbxSlotName;
    private JPanel pnlSlot;
    private JPanel pnlSlotHolder;
    private JPanel pnlCheckBox;
    private JPanel pnlSlotRadio;
    private JLabel lblSlotName;
    private JLabel lblSlotConfiguration;
    private HyperlinkLabel lblCreateWebApp;
    private JCheckBox chkOpenBrowser;
    private JLabel lblSlotHover;
    private HideableDecorator slotDecorator;

    // presenter
    private WebAppConfiguration webAppConfiguration;

    public WebAppSlimSettingPanel(@NotNull Project project, @NotNull WebAppConfiguration webAppConfiguration) {
        super(project);
        this.webAppConfiguration = webAppConfiguration;
        this.presenter = new WebAppDeployViewPresenterSlim();
        this.presenter.onAttachView(this);
        // Slot
        final ButtonGroup slotButtonGroup = new ButtonGroup();
        slotButtonGroup.add(rbtExistingSlot);
        slotButtonGroup.add(rbtNewSlot);
        rbtExistingSlot.addActionListener(e -> toggleSlotType(true));
        rbtNewSlot.addActionListener(e -> toggleSlotType(false));

        chkDeployToSlot.addActionListener(e -> toggleSlotPanel(chkDeployToSlot.isSelected()));

        cbxWebApp.addActionListener(e -> selectWebApp());
        cbxWebApp.setRenderer(new WebAppCombineBoxRender(cbxWebApp));
        // Set the editor of combobox, otherwise it will use box render when popup is invisible, which may render the
        // combobox to twoline
        cbxWebApp.setEditor(new ComboBoxEditor() {
            Object item;
            JLabel label = new JLabel();

            @Override
            public Component getEditorComponent() {
                return label;
            }

            @Override
            public void setItem(Object anObject) {
                item = anObject;
                if (anObject == null) {
                    return;
                } else if (anObject instanceof String) {
                    label.setText((String) anObject);
                } else {
                    ResourceEx<WebApp> webApp = (ResourceEx<WebApp>) anObject;
                    label.setText(webApp.getResource().name());
                }
            }

            @Override
            public Object getItem() {
                return item;
            }

            @Override
            public void selectAll() {
                return;
            }

            @Override
            public void addActionListener(ActionListener l) {
                return;
            }

            @Override
            public void removeActionListener(ActionListener l) {
                return;
            }
        });
        cbxWebApp.setEditable(true);

        cbArtifact.addActionListener(e -> artifactActionPeformed((Artifact) cbArtifact.getSelectedItem()));

        cbArtifact.setRenderer(new ListCellRendererWrapper<Artifact>() {
            @Override
            public void customize(JList list, Artifact artifact, int index, boolean isSelected, boolean cellHasFocus) {
                if (artifact != null) {
                    setIcon(artifact.getArtifactType().getIcon());
                    setText(artifact.getName());
                }
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

        slotDecorator = new HideableDecorator(pnlSlotHolder, DEPLOYMENT_SLOT, true);
        slotDecorator.setContentComponent(pnlSlot);
        slotDecorator.setOn(webAppConfiguration.isSlotPanelVisible());
    }

    private void selectWebApp() {
        Object value = cbxWebApp.getSelectedItem();
        if (Comparing.equal(CREATE_NEW_WEBAPP, value) && !refreshingWebApp) {
            createNewWebApp();
        } else if (value == null || value instanceof String) {
            return;
        } else {
            chkDeployToSlot.setEnabled(true);
            selectedWebApp = (ResourceEx<WebApp>) cbxWebApp.getSelectedItem();
            presenter.onLoadDeploymentSlots(selectedWebApp);
        }
    }

    @NotNull
    @Override
    public String getPanelName() {
        return "Deploy to Azure";
    }

    @Override
    public void disposeEditor() {
    }

    @Override
    protected void resetFromConfig(@NotNull WebAppConfiguration configuration) {
        refreshWebApps(false);
        if (configuration.getWebAppId() != null && webAppConfiguration.isDeployToSlot()) {
            toggleSlotPanel(true);
            chkDeployToSlot.setSelected(true);
            final boolean useNewDeploymentSlot = Comparing.equal(configuration.getSlotName(),
                Constants.CREATE_NEW_SLOT);
            rbtNewSlot.setSelected(useNewDeploymentSlot);
            rbtExistingSlot.setSelected(!useNewDeploymentSlot);
            toggleSlotType(!useNewDeploymentSlot);
            presenter.onLoadDeploymentSlots(configuration.getSubscriptionId(), configuration.getWebAppId());
        } else {
            toggleSlotPanel(false);
            chkDeployToSlot.setSelected(false);
        }
        final DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        final String defaultSlotName = StringUtils.isEmpty(webAppConfiguration.getNewSlotName()) ?
            String.format(DEFAULT_SLOT_NAME, df.format(new Date())) : webAppConfiguration.getNewSlotName();
        txtNewSlotName.setText(defaultSlotName);
        chkToRoot.setSelected(configuration.isDeployToRoot());
        chkBuild.setSelected(configuration.isCompileBeforeDeploy());
        chkOpenBrowser.setSelected(configuration.isOpenBrowserAfterDeployment());
        slotDecorator.setOn(configuration.isSlotPanelVisible());
    }

    @Override
    protected void apply(@NotNull WebAppConfiguration configuration) {
        final String targetName = getTargetName();
        configuration.setTargetPath(getTargetPath());
        configuration.setTargetName(targetName);
        configuration.setCreatingNew(false);
        configuration.setWebAppId(selectedWebApp == null ? null : selectedWebApp.getResource().id());
        configuration.setSubscriptionId(selectedWebApp == null ? null : selectedWebApp.getSubscriptionId());
        configuration.setDeployToSlot(chkDeployToSlot.isSelected());
        configuration.setCompileBeforeDeploy(chkBuild.isSelected());
        configuration.setSlotPanelVisible(slotDecorator.isExpanded());
        chkToRoot.setVisible(isAbleToDeployToRoot(targetName));
        chkBuild.setVisible(MavenRunTaskUtil.isMavenProject(project));
        toggleSlotPanel(configuration.isDeployToSlot() && selectedWebApp != null);
        if (chkDeployToSlot.isSelected()) {
            configuration.setDeployToSlot(true);
            configuration.setSlotName(cbxSlotName.getSelectedItem() == null ? "" :
                cbxSlotName.getSelectedItem().toString());
            if (rbtNewSlot.isSelected()) {
                configuration.setSlotName(Constants.CREATE_NEW_SLOT);
                configuration.setNewSlotName(txtNewSlotName.getText());
                configuration.setNewSlotConfigurationSource((String) cbxSlotConfigurationSource.getSelectedItem());
            }
        } else {
            configuration.setDeployToSlot(false);
        }
        configuration.setDeployToRoot(chkToRoot.isVisible() && chkToRoot.isSelected());
        configuration.setCompileBeforeDeploy(chkBuild.isSelected());
        configuration.setOpenBrowserAfterDeployment(chkOpenBrowser.isSelected());
    }

    private boolean isAbleToDeployToRoot(final String targetName) {
        if (selectedWebApp == null) {
            return false;
        }
        final WebApp app = selectedWebApp.getResource();
        final boolean isDeployingWar =
            MavenRunTaskUtil.getFileType(targetName).equalsIgnoreCase(MavenConstants.TYPE_WAR);
        return isDeployingWar && (app.operatingSystem() == OperatingSystem.WINDOWS ||
            !Constants.LINUX_JAVA_SE_RUNTIME.equalsIgnoreCase(app.linuxFxVersion()));
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlRoot;
    }

    @NotNull
    @Override
    protected JComboBox<Artifact> getCbArtifact() {
        return cbArtifact;
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return lblArtifact;
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return cbMavenProject;
    }

    @NotNull
    @Override
    protected JLabel getLblMavenProject() {
        return lblMavenProject;
    }

    private void createNewWebApp() {
        final WebAppCreationDialog dialog = new WebAppCreationDialog(this.webAppConfiguration);
        dialog.pack();
        dialog.setLocationRelativeTo(this.getMainPanel());
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                WebApp newWebApp = dialog.getCreatedWebApp();
                if (newWebApp != null) {
                    webAppConfiguration.setWebAppId(newWebApp.id());
                    refreshWebApps(true);
                } else {
                    refreshWebApps(false);
                }
            }
        });
        dialog.setVisible(true);
    }

    private void toggleSlotPanel(boolean isDeployToSlot) {
        isDeployToSlot &= (selectedWebApp != null);
        rbtNewSlot.setEnabled(isDeployToSlot);
        rbtExistingSlot.setEnabled(isDeployToSlot);
        lblSlotName.setEnabled(isDeployToSlot);
        lblSlotConfiguration.setEnabled(isDeployToSlot);
        cbxSlotName.setEnabled(isDeployToSlot);
        txtNewSlotName.setEnabled(isDeployToSlot);
        cbxSlotConfigurationSource.setEnabled(isDeployToSlot);
    }

    private void toggleSlotType(final boolean isExistingSlot) {
        cbxSlotName.setVisible(isExistingSlot);
        cbxSlotName.setEnabled(isExistingSlot);
        txtNewSlotName.setVisible(!isExistingSlot);
        txtNewSlotName.setEnabled(!isExistingSlot);
        lblSlotConfiguration.setVisible(!isExistingSlot);
        cbxSlotConfigurationSource.setVisible(!isExistingSlot);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        lblCreateWebApp = new HyperlinkLabel("No available webapp, create a new one");
        lblCreateWebApp.addHyperlinkListener(e -> createNewWebApp());

        lblSlotHover = new JLabel(AllIcons.General.Information);
        lblSlotHover.setToolTipText(DEPLOYMENT_SLOT_HOVER);
    }

    public void refreshWebApps(boolean force) {
        cbxWebApp.removeAllItems();
        cbxWebApp.setEnabled(false);
        cbxWebApp.addItem(REFRESHING_WEBAPP);
        presenter.loadWebApps(force);
    }

    @Override
    public synchronized void fillWebApps(List<ResourceEx<WebApp>> webAppLists) {
        refreshingWebApp = true;
        cbxWebApp.removeAllItems();
        webAppLists = webAppLists.stream()
            .filter(resource -> WebAppUtils.isJavaWebApp(resource.getResource()))
            .sorted((a, b) -> a.getSubscriptionId().compareToIgnoreCase(b.getSubscriptionId()))
            .collect(Collectors.toList());
        if (webAppLists.size() == 0) {
            lblCreateWebApp.setVisible(true);
            cbxWebApp.setVisible(false);
        } else {
            lblCreateWebApp.setVisible(false);
            cbxWebApp.setVisible(true);
            cbxWebApp.addItem(CREATE_NEW_WEBAPP);
            webAppLists.forEach(webAppResourceEx -> cbxWebApp.addItem(webAppResourceEx));
            // Find webapp which id equals to configuration, or use the first available one.
            final ResourceEx<WebApp> selectWebApp = webAppLists.stream()
                .filter(webAppResourceEx -> webAppResourceEx.getResource().id().equals(webAppConfiguration.getWebAppId()))
                .findFirst().orElse(webAppLists.get(0));
            cbxWebApp.setSelectedItem(selectWebApp);
        }
        refreshingWebApp = false;
        cbxWebApp.setEnabled(true);
        selectWebApp();
    }

    @Override
    public synchronized void fillDeploymentSlots(List<DeploymentSlot> slotList) {
        cbxSlotName.removeAllItems();
        cbxSlotConfigurationSource.removeAllItems();

        final List<String> configurationSources = new ArrayList<String>();
        final List<String> deploymentSlots = new ArrayList<String>();
        configurationSources.add(Constants.DO_NOT_CLONE_SLOT_CONFIGURATION);
        configurationSources.add(selectedWebApp.getResource().name());
        slotList.stream().filter(slot -> slot != null).forEach(slot -> {
            deploymentSlots.add(slot.name());
            configurationSources.add(slot.name());
        });
        deploymentSlots.forEach(s -> {
            cbxSlotName.addItem(s);
            if (Comparing.equal(s, webAppConfiguration.getSlotName())) {
                cbxSlotName.setSelectedItem(s);
            }
        });
        configurationSources.forEach(c -> {
            cbxSlotConfigurationSource.addItem(c);
            if (Comparing.equal(c, webAppConfiguration.getNewSlotConfigurationSource())) {
                cbxSlotConfigurationSource.setSelectedItem(c);
            }
        });
    }

    class WebAppCombineBoxRender extends ListCellRendererWrapper {

        private final JComboBox comboBox;
        private final int cellHeight;
        private final String TEMPLATE_STRING = "<html><div>TEMPLATE</div><small>TEMPLATE</small></html>";

        public WebAppCombineBoxRender(JComboBox comboBox) {
            this.comboBox = comboBox;
            JLabel template = new JLabel(TEMPLATE_STRING);
            //Create a multi-line jlabel and calculate its preferred size
            this.cellHeight = template.getPreferredSize().height;
        }

        @Override
        public void customize(JList jList, Object value, int i, boolean b, boolean b1) {
            if (value == null) {
                return;
            } else if (value instanceof String) {
                setText(getStringLabelText((String) value));
            } else {
                final ResourceEx<WebApp> webApp = (ResourceEx<WebApp>) value;
                setText(getWebAppLabelText(webApp.getResource()));
            }
            jList.setFixedCellHeight(cellHeight);
        }

        private String getStringLabelText(String message) {
            return comboBox.isPopupVisible() ? String.format("<html><div>%s</div><small></small></html>",
                message) : message;
        }

        private String getWebAppLabelText(WebApp webApp) {
            final String webAppName = webApp.name();
            final String os = StringUtils.capitalize(webApp.operatingSystem().toString());
            final String runtime = WebAppUtils.getJavaRuntime(webApp);
            final String resourceGroup = webApp.resourceGroupName();

            return comboBox.isPopupVisible() ? String.format("<html><div>%s</div></div><small>OS:%s Runtime:%s " +
                "ResourceGroup:%s</small></html>", webAppName, os, runtime, resourceGroup) : webAppName;
        }
    }
}
