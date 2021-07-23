/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.slimui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppComboBox;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppComboBoxModel;
import com.microsoft.azure.toolkit.intellij.webapp.runner.Constants;
import com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel.DO_NOT_CLONE_SLOT_CONFIGURATION;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class WebAppSlimSettingPanel extends AzureSettingPanel<WebAppConfiguration> implements WebAppDeployMvpViewSlim {
    private static final String[] FILE_NAME_EXT = {"war", "jar", "ear"};
    private static final String DEPLOYMENT_SLOT = "Deployment Slot";
    private static final String DEFAULT_SLOT_NAME = "slot-%s";

    private final WebAppDeployViewPresenterSlim presenter;

    private JPanel pnlSlotCheckBox;
    private JTextField txtNewSlotName;
    private JComboBox cbxSlotConfigurationSource;
    private JCheckBox chkDeployToSlot;
    private JCheckBox chkToRoot;
    private JPanel pnlRoot;
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
    private JCheckBox chkOpenBrowser;
    private HyperlinkLabel lblNewSlot;
    private JPanel pnlExistingSlot;
    private JButton btnSlotHover;
    private AzureArtifactComboBox comboBoxArtifact;
    private WebAppComboBox comboBoxWebApp;
    private JLabel lblArtifact;
    private JLabel lblWebApp;
    private final HideableDecorator slotDecorator;

    public WebAppSlimSettingPanel(@NotNull Project project, @NotNull WebAppConfiguration webAppConfiguration) {
        super(project, false);
        this.presenter = new WebAppDeployViewPresenterSlim();
        this.presenter.onAttachView(this);

        final ButtonGroup slotButtonGroup = new ButtonGroup();
        slotButtonGroup.add(rbtNewSlot);
        slotButtonGroup.add(rbtExistingSlot);
        rbtExistingSlot.addActionListener(e -> toggleSlotType(true));
        rbtNewSlot.addActionListener(e -> toggleSlotType(false));

        chkDeployToSlot.addActionListener(e -> toggleSlotPanel(chkDeployToSlot.isSelected()));

        final Icon informationIcon = AllIcons.General.ContextHelp;
        btnSlotHover.setIcon(informationIcon);
        btnSlotHover.setHorizontalAlignment(SwingConstants.CENTER);
        btnSlotHover.setPreferredSize(new Dimension(informationIcon.getIconWidth(), informationIcon.getIconHeight()));
        btnSlotHover.setToolTipText(message("webapp.deploy.hint.deploymentSlot"));
        btnSlotHover.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                btnSlotHover.setBorderPainted(true);
                final MouseEvent phantom = new MouseEvent(btnSlotHover, MouseEvent.MOUSE_ENTERED,
                                                          System.currentTimeMillis(), 0, 10, 10, 0, false);
                DefaultLoader.getIdeHelper().invokeLater(() -> IdeTooltipManager.getInstance().eventDispatched(phantom));
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                btnSlotHover.setBorderPainted(false);
                IdeTooltipManager.getInstance().dispose();
            }
        });

        final JLabel labelForNewSlotName = new JLabel("Slot Name");
        labelForNewSlotName.setLabelFor(txtNewSlotName);
        final JLabel labelForExistingSlotName = new JLabel("Slot Name");
        labelForExistingSlotName.setLabelFor(cbxSlotName);

        lblArtifact.setLabelFor(comboBoxArtifact);
        lblWebApp.setLabelFor(comboBoxWebApp);

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
        presenter.onDetachView();
    }

    @Override
    public synchronized void fillDeploymentSlots(List<IWebAppDeploymentSlot> slotList, @NotNull final WebAppComboBoxModel selectedWebApp) {
        final String defaultSlot = (String) cbxSlotName.getSelectedItem();
        final String defaultConfigurationSource = (String) cbxSlotConfigurationSource.getSelectedItem();
        cbxSlotName.removeAllItems();
        cbxSlotConfigurationSource.removeAllItems();
        cbxSlotConfigurationSource.addItem(DO_NOT_CLONE_SLOT_CONFIGURATION);
        cbxSlotConfigurationSource.addItem(selectedWebApp.getAppName());
        slotList.stream().filter(Objects::nonNull).forEach(slot -> {
            cbxSlotName.addItem(slot.name());
            cbxSlotConfigurationSource.addItem(slot.name());
        });
        setComboBoxDefaultValue(cbxSlotName, defaultSlot);
        setComboBoxDefaultValue(cbxSlotConfigurationSource, defaultConfigurationSource);
        final boolean existDeploymentSlot = slotList.size() > 0;
        lblNewSlot.setVisible(!existDeploymentSlot);
        cbxSlotName.setVisible(existDeploymentSlot);
    }

    private void setComboBoxDefaultValue(JComboBox comboBox, Object value) {
        UIUtils.listComboBoxItems(comboBox).stream().filter(item -> item.equals(value)).findFirst().ifPresent(defaultItem -> comboBox.setSelectedItem(value));
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlRoot;
    }

    @NotNull
    @Override
    protected JComboBox<Artifact> getCbArtifact() {
        return new ComboBox<>();
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return new JLabel();
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return new ComboBox<>();
    }

    @NotNull
    @Override
    protected JLabel getLblMavenProject() {
        return new JLabel();
    }

    @Override
    protected void resetFromConfig(@NotNull WebAppConfiguration configuration) {
        if (!StringUtils.isAllEmpty(configuration.getWebAppName(), configuration.getWebAppId())) {
            final WebAppComboBoxModel configModel = new WebAppComboBoxModel(configuration.getModel());
            comboBoxWebApp.setConfigModel(configModel);
        }
        comboBoxWebApp.refreshItems();
        if (configuration.getAzureArtifactType() != null) {
            lastSelectedAzureArtifact = AzureArtifactManager
                    .getInstance(project)
                    .getAzureArtifactById(configuration.getAzureArtifactType(), configuration.getArtifactIdentifier());
            comboBoxArtifact.refreshItems(lastSelectedAzureArtifact);
        } else {
            comboBoxArtifact.refreshItems();
        }
        if (configuration.getWebAppId() != null && configuration.isDeployToSlot()) {
            toggleSlotPanel(true);
            chkDeployToSlot.setSelected(true);
            final boolean useNewDeploymentSlot = StringUtils.equals(configuration.getSlotName(), Constants.CREATE_NEW_SLOT);
            rbtNewSlot.setSelected(useNewDeploymentSlot);
            rbtExistingSlot.setSelected(!useNewDeploymentSlot);
            toggleSlotType(!useNewDeploymentSlot);
            txtNewSlotName.setText(configuration.getNewSlotName());
            cbxSlotName.addItem(useNewDeploymentSlot ? configuration.getNewSlotName() : configuration.getSlotName());
            cbxSlotConfigurationSource.addItem(configuration.getNewSlotConfigurationSource());
        } else {
            toggleSlotPanel(false);
            chkDeployToSlot.setSelected(false);
        }
        final DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        final String defaultSlotName = StringUtils.isEmpty(configuration.getNewSlotName()) ?
                String.format(DEFAULT_SLOT_NAME, df.format(new Date())) : configuration.getNewSlotName();
        txtNewSlotName.setText(defaultSlotName);
        chkToRoot.setSelected(configuration.isDeployToRoot());
        chkOpenBrowser.setSelected(configuration.isOpenBrowserAfterDeployment());
        slotDecorator.setOn(configuration.isSlotPanelVisible());
    }

    private WebAppComboBoxModel getSelectedWebApp() {
        final Object selectedItem = comboBoxWebApp.getSelectedItem();
        return selectedItem instanceof WebAppComboBoxModel ? (WebAppComboBoxModel) selectedItem : null;
    }

    @Override
    protected void apply(@NotNull WebAppConfiguration configuration) {
        final WebAppComboBoxModel selectedWebApp = getSelectedWebApp();
        if (selectedWebApp != null) {
            configuration.saveModel(selectedWebApp);
        }
        configuration.saveArtifact(comboBoxArtifact.getValue());
        configuration.setDeployToSlot(chkDeployToSlot.isSelected());
        configuration.setSlotPanelVisible(slotDecorator.isExpanded());
        chkToRoot.setVisible(isAbleToDeployToRoot(comboBoxArtifact.getValue()));
        toggleSlotPanel(configuration.isDeployToSlot() && selectedWebApp != null);
        if (chkDeployToSlot.isSelected()) {
            configuration.setDeployToSlot(true);
            configuration.setSlotName(cbxSlotName.getSelectedItem() == null ? "" : cbxSlotName.getSelectedItem().toString());
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
        // hot fix, to avoid similar cases, prefer to refactor this code with common factory
        if (ApplicationManager.getApplication().isDispatchThread()) {
            syncBeforeRunTasks(comboBoxArtifact.getValue(), configuration);
        } else {
            ApplicationManager.getApplication().invokeLater(() ->
                syncBeforeRunTasks(comboBoxArtifact.getValue(), configuration)
            );
        }
    }

    private boolean isAbleToDeployToRoot(final AzureArtifact azureArtifact) {
        final WebAppComboBoxModel selectedWebApp = getSelectedWebApp();
        if (selectedWebApp == null || azureArtifact == null) {
            return false;
        }
        final WebContainer webContainer = selectedWebApp.getRuntime().getWebContainer();
        final String packaging = AzureArtifactManager.getInstance(project).getPackaging(azureArtifact);
        final boolean isDeployingWar = StringUtils.equalsAnyIgnoreCase(packaging, MavenConstants.TYPE_WAR, "ear");
        return isDeployingWar && StringUtils.containsAnyIgnoreCase(webContainer.getValue(), "tomcat", "jboss");
    }

    private void toggleSlotPanel(boolean slot) {
        final boolean isDeployToSlot = slot && (getSelectedWebApp() != null);
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
        lblNewSlot = new HyperlinkLabel(message("webapp.deploy.noDeploymentSlot"));
        lblNewSlot.addHyperlinkListener(e -> rbtNewSlot.doClick());

        comboBoxWebApp = new WebAppComboBox(project);
        comboBoxWebApp.addItemListener(e -> loadDeploymentSlot(getSelectedWebApp()));

        comboBoxArtifact = new AzureArtifactComboBox(this.project);
        comboBoxArtifact.setFileFilter(virtualFile -> {
            final String ext = FileNameUtils.getExtension(virtualFile.getPath());
            return ArrayUtils.contains(FILE_NAME_EXT, ext);
        });

    }

    private void loadDeploymentSlot(WebAppComboBoxModel selectedWebApp) {
        if (selectedWebApp == null) {
            return;
        }
        if (selectedWebApp.isNewCreateResource()) {
            chkDeployToSlot.setEnabled(false);
            chkDeployToSlot.setSelected(false);
        } else {
            chkDeployToSlot.setEnabled(true);
            presenter.onLoadDeploymentSlots(comboBoxWebApp.getValue());
        }
    }
}
