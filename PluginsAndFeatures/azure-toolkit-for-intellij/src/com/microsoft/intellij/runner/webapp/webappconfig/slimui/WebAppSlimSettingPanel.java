package com.microsoft.intellij.runner.webapp.webappconfig.slimui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.PopupMenuListenerAdapter;
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
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.EventListenerList;
import javax.swing.event.PopupMenuEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WebAppSlimSettingPanel extends AzureSettingPanel<WebAppConfiguration> implements WebAppDeployMvpViewSlim {

    private static final String DEPLOYMENT_SLOT = "Deployment Slot";
    private static final String DEFAULT_SLOT_NAME = "slot-%s";
    private static final String CREATE_NEW_WEBAPP = "Create New WebApp";
    private static final String REFRESHING_WEBAPP = "Refreshing...";
    private static final String DEPLOYMENT_SLOT_HOVER = "Deployment slots are live apps with their own hostnames. App" +
            " content and configurations elements can be swapped between two deployment slots, including the production " +
            "slot.";

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
    private HyperlinkLabel lblNewSlot;
    private JPanel pnlExistingSlot;
    private JButton btnSlotHover;
    private HideableDecorator slotDecorator;

    // presenter
    private WebAppConfiguration webAppConfiguration;

    public WebAppSlimSettingPanel(@NotNull Project project, @NotNull WebAppConfiguration webAppConfiguration) {
        super(project);
        this.webAppConfiguration = webAppConfiguration;
        this.presenter = new WebAppDeployViewPresenterSlim();
        this.presenter.onAttachView(this);

        final ButtonGroup slotButtonGroup = new ButtonGroup();
        slotButtonGroup.add(rbtNewSlot);
        slotButtonGroup.add(rbtExistingSlot);
        rbtExistingSlot.addActionListener(e -> toggleSlotType(true));
        rbtNewSlot.addActionListener(e -> toggleSlotType(false));

        chkDeployToSlot.addActionListener(e -> toggleSlotPanel(chkDeployToSlot.isSelected()));

        cbxWebApp.setRenderer(new WebAppCombineBoxRender(cbxWebApp));
        cbxWebApp.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                selectWebApp();
            }
        });
        // Set the editor of combobox, otherwise it will use box render when popup is invisible, which may render the
        // combobox to twoline
        cbxWebApp.setEditor(new ComboBoxEditor() {
            private Object item;
            private JLabel label = new JLabel();
            private EventListenerList listenerList = new EventListenerList();

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
                label.getAccessibleContext().setAccessibleName(label.getText());
                label.getAccessibleContext().setAccessibleDescription(label.getText());
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
                listenerList.add(ActionListener.class, l);
            }

            @Override
            public void removeActionListener(ActionListener l) {
                listenerList.remove(ActionListener.class, l);
            }
        });

        Icon informationIcon = AllIcons.General.Information;
        btnSlotHover.setIcon(informationIcon);
        btnSlotHover.setHorizontalAlignment(SwingConstants.CENTER);
        btnSlotHover.setPreferredSize(new Dimension(informationIcon.getIconWidth(), informationIcon.getIconHeight()));
        btnSlotHover.setToolTipText(DEPLOYMENT_SLOT_HOVER);
        btnSlotHover.addFocusListener(new FocusListener() {

            Subscription subscription;

            @Override
            public void focusGained(FocusEvent focusEvent) {
                btnSlotHover.setBorderPainted(true);
                MouseEvent phantom = new MouseEvent(btnSlotHover, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(),
                        0, 10, 10, 0, false);
                IdeTooltipManager.getInstance().eventDispatched(phantom);
                if (subscription != null) {
                    subscription.unsubscribe();
                }
                subscription = Observable.timer(2, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.newThread())
                        .subscribe(next -> {
                            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == btnSlotHover) {
                                focusGained(focusEvent);
                            }
                        });
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                btnSlotHover.setBorderPainted(false);
                IdeTooltipManager.getInstance().dispose();
            }
        });

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

        JLabel labelForNewSlotName = new JLabel("Slot Name");
        labelForNewSlotName.setLabelFor(txtNewSlotName);
        JLabel labelForExistingSlotName = new JLabel("Slot Name");
        labelForExistingSlotName.setLabelFor(cbxSlotName);

        slotDecorator = new HideableDecorator(pnlSlotHolder, DEPLOYMENT_SLOT, true);
        slotDecorator.setContentComponent(pnlSlot);
        slotDecorator.setOn(webAppConfiguration.isSlotPanelVisible());
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
    public synchronized void fillWebApps(List<ResourceEx<WebApp>> webAppLists) {
        cbxWebApp.removeAllItems();
        webAppLists = webAppLists.stream()
                .filter(resource -> WebAppUtils.isJavaWebApp(resource.getResource()))
                .sorted((a, b) -> a.getResource().name().compareToIgnoreCase(b.getResource().name()))
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
        selectWebApp();
        cbxWebApp.setEnabled(true);
    }

    @Override
    public synchronized void fillDeploymentSlots(List<DeploymentSlot> slotList) {
        cbxSlotName.removeAllItems();
        cbxSlotConfigurationSource.removeAllItems();

        cbxSlotConfigurationSource.addItem(Constants.DO_NOT_CLONE_SLOT_CONFIGURATION);
        cbxSlotConfigurationSource.addItem(selectedWebApp.getResource().name());
        slotList.stream().filter(slot -> slot != null).forEach(slot -> {
            cbxSlotName.addItem(slot.name());
            cbxSlotConfigurationSource.addItem(slot.name());
            if (Comparing.equal(slot.name(), webAppConfiguration.getSlotName())) {
                cbxSlotName.setSelectedItem(slot.name());
            }
            if (Comparing.equal(slot.name(), webAppConfiguration.getNewSlotConfigurationSource())) {
                cbxSlotConfigurationSource.setSelectedItem(slot.name());
            }
        });

        boolean existDeploymentSlot = slotList.size() > 0;
        lblNewSlot.setVisible(!existDeploymentSlot);
        cbxSlotName.setVisible(existDeploymentSlot);
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
        } else {
            toggleSlotPanel(false);
            chkDeployToSlot.setSelected(false);
        }
        final DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        final String defaultSlotName = StringUtils.isEmpty(webAppConfiguration.getNewSlotName()) ?
                String.format(DEFAULT_SLOT_NAME, df.format(new Date())) : webAppConfiguration.getNewSlotName();
        txtNewSlotName.setText(defaultSlotName);
        chkToRoot.setSelected(configuration.isDeployToRoot());
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
        configuration.setSlotPanelVisible(slotDecorator.isExpanded());
        chkToRoot.setVisible(isAbleToDeployToRoot(targetName));
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
        configuration.setOpenBrowserAfterDeployment(chkOpenBrowser.isSelected());
    }

    private void selectWebApp() {
        Object value = cbxWebApp.getSelectedItem();
        if (value != null && value instanceof ResourceEx) {
            chkDeployToSlot.setEnabled(true);
            selectedWebApp = (ResourceEx<WebApp>) cbxWebApp.getSelectedItem();
            presenter.onLoadDeploymentSlots(selectedWebApp);
        } else if (Comparing.equal(CREATE_NEW_WEBAPP, value)) {
            // Create new web app
            cbxWebApp.setSelectedItem(null);
            ApplicationManager.getApplication().invokeLater(()->createNewWebApp());
        }
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
        pnlExistingSlot.setVisible(isExistingSlot);
        pnlExistingSlot.setEnabled(isExistingSlot);
        txtNewSlotName.setVisible(!isExistingSlot);
        txtNewSlotName.setEnabled(!isExistingSlot);
        lblSlotConfiguration.setVisible(!isExistingSlot);
        cbxSlotConfigurationSource.setVisible(!isExistingSlot);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        lblCreateWebApp = new HyperlinkLabel("No available webapp, click to create a new one");
        lblCreateWebApp.addHyperlinkListener(e -> createNewWebApp());

        lblNewSlot = new HyperlinkLabel("No available deployment slot, click to create a new one");
        lblNewSlot.addHyperlinkListener(e -> rbtNewSlot.doClick());
    }

    private void refreshWebApps(boolean force) {
        cbxWebApp.removeAllItems();
        cbxWebApp.setEnabled(false);
        cbxWebApp.addItem(REFRESHING_WEBAPP);
        presenter.loadWebApps(force);
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
                // For label in combobox textfield, just show webapp name
                final String text = i>=0 ? getWebAppLabelText(webApp.getResource()) : webApp.getResource().name();
                setText(text);
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
