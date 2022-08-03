/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.slimui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.ide.appservice.model.AzureArtifactConfig;
import com.microsoft.azure.toolkit.ide.appservice.model.DeploymentSlotConfig;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppDeployRunConfigurationModel;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactType;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.intellij.ui.util.UIUtils;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenConstants;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class WebAppDeployConfigurationPanel extends JPanel implements AzureFormPanel<WebAppDeployRunConfigurationModel> {
    private static final String[] FILE_NAME_EXT = {"war", "jar", "ear"};
    private static final String DEPLOYMENT_SLOT = "&Deployment Slot";
    private static final String DEFAULT_SLOT_NAME = "slot-%s";

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
    private JLabel lblArtifact;
    private JLabel lblWebApp;
    private WebAppComboBox comboBoxWebApp;
    private final HideableDecorator slotDecorator;

    private final Project project;

    public WebAppDeployConfigurationPanel(@NotNull Project project) {
        super();
        this.project = project;
        $$$setupUI$$$();
        comboBoxWebApp.addItemListener(e -> loadDeploymentSlot(getSelectedWebApp()));
        comboBoxArtifact.addItemListener(e -> chkToRoot.setVisible(isAbleToDeployToRoot(comboBoxArtifact.getValue())));

        final ButtonGroup slotButtonGroup = new ButtonGroup();
        slotButtonGroup.add(rbtNewSlot);
        slotButtonGroup.add(rbtExistingSlot);
        rbtExistingSlot.addItemListener(e -> toggleSlotType(true));
        rbtNewSlot.addItemListener(e -> toggleSlotType(false));
        chkDeployToSlot.addItemListener(e -> toggleSlotPanel(chkDeployToSlot.isSelected()));

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
                AzureTaskManager.getInstance().runLater(() -> IdeTooltipManager.getInstance().eventDispatched(phantom));
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

        final DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        txtNewSlotName.setText(String.format(DEFAULT_SLOT_NAME, df.format(new Date())));

        slotDecorator = new HideableDecorator(pnlSlotHolder, DEPLOYMENT_SLOT, true);
        slotDecorator.setContentComponent(pnlSlot);
    }

    private void setComboBoxDefaultValue(JComboBox comboBox, Object value) {
        UIUtils.listComboBoxItems(comboBox).stream().filter(item -> item.equals(value)).findFirst().ifPresent(defaultItem -> comboBox.setSelectedItem(value));
    }

    private WebAppConfig getSelectedWebApp() {
        return comboBoxWebApp.getValue();
    }

    private boolean isAbleToDeployToRoot(final AzureArtifact azureArtifact) {
        final WebAppConfig selectedWebApp = getSelectedWebApp();
        if (selectedWebApp == null || azureArtifact == null) {
            return false;
        }
        final String webContainer = Optional.ofNullable(selectedWebApp.getRuntime())
                .map(Runtime::getWebContainer).map(WebContainer::getValue).orElse(StringUtils.EMPTY);
        final String packaging = AzureArtifactManager.getInstance(project).getPackaging(azureArtifact);
        final boolean isDeployingWar = StringUtils.equalsAnyIgnoreCase(packaging, MavenConstants.TYPE_WAR, "ear");
        return isDeployingWar && StringUtils.containsAny(webContainer.toLowerCase(), "tomcat", "jboss");
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
        comboBoxWebApp.reloadItems();

        comboBoxArtifact = new AzureArtifactComboBox(this.project);
        comboBoxArtifact.setFileFilter(virtualFile -> {
            final String ext = FileNameUtils.getExtension(virtualFile.getPath());
            return ArrayUtils.contains(FILE_NAME_EXT, ext);
        });
        comboBoxArtifact.reloadItems();
    }

    private void loadDeploymentSlot(WebAppConfig selectedWebApp) {
        if (selectedWebApp == null) {
            return;
        }
        if (StringUtils.isEmpty(selectedWebApp.getResourceId())) {
            chkDeployToSlot.setEnabled(false);
            chkDeployToSlot.setSelected(false);
        } else {
            chkDeployToSlot.setEnabled(true);
            Mono.fromCallable(() -> Azure.az(AzureWebApp.class).webApp(selectedWebApp.getResourceId()).slots().list())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(slots -> AzureTaskManager.getInstance().runLater(() -> fillDeploymentSlots(slots, selectedWebApp), AzureTask.Modality.ANY));
        }
    }

    private synchronized void fillDeploymentSlots(List<WebAppDeploymentSlot> slotList, @NotNull final WebAppConfig selectedWebApp) {
        final String defaultSlot = (String) cbxSlotName.getSelectedItem();
        final String defaultConfigurationSource = (String) cbxSlotConfigurationSource.getSelectedItem();
        cbxSlotName.removeAllItems();
        cbxSlotConfigurationSource.removeAllItems();
        cbxSlotConfigurationSource.addItem(AzureWebAppMvpModel.DO_NOT_CLONE_SLOT_CONFIGURATION);
        cbxSlotConfigurationSource.addItem(selectedWebApp.getName());
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

    @Override
    public void setValue(WebAppDeployRunConfigurationModel data) {
        // artifact
        Optional.ofNullable(data.getArtifactConfig())
                .filter(config -> !StringUtils.isAnyEmpty(config.getArtifactIdentifier(), config.getArtifactType()))
                .map(config -> AzureArtifactManager.getInstance(this.project)
                        .getAzureArtifactById(AzureArtifactType.valueOf(config.getArtifactType()), config.getArtifactIdentifier()))
                .ifPresent(artifact -> comboBoxArtifact.setArtifact(artifact));
        // web app
        Optional.ofNullable(data.getWebAppConfig()).ifPresent(webApp -> {
            comboBoxWebApp.setConfigModel(webApp);
            comboBoxWebApp.setValue(new AzureComboBox.ItemReference<>(item -> WebAppConfig.isSameApp(item, webApp)));
            toggleSlotPanel(webApp.getDeploymentSlot() != null);
            Optional.ofNullable(webApp.getDeploymentSlot()).ifPresent(slot -> {
                chkDeployToSlot.setSelected(true);
                rbtNewSlot.setSelected(slot.isNewCreate());
                rbtExistingSlot.setSelected(!slot.isNewCreate());
                toggleSlotType(!slot.isNewCreate());
                if (slot.isNewCreate()) {
                    txtNewSlotName.setText(slot.getName());
                    cbxSlotConfigurationSource.addItem(slot.getConfigurationSource());
                    cbxSlotConfigurationSource.setSelectedItem(slot.getConfigurationSource());
                } else {
                    cbxSlotName.addItem(slot.getName());
                    cbxSlotName.setSelectedItem(slot.getName());
                }
            });
        });
        // configuration
        chkToRoot.setSelected(data.isDeployToRoot());
        chkOpenBrowser.setSelected(data.isOpenBrowserAfterDeployment());
        slotDecorator.setOn(data.isSlotPanelVisible());
    }

    @Override
    public WebAppDeployRunConfigurationModel getValue() {
        final AzureArtifact artifact = comboBoxArtifact.getValue();
        final AzureArtifactConfig artifactConfig = artifact == null ? null :
                AzureArtifactConfig.builder().artifactType(artifact.getType().name())
                        .artifactIdentifier(AzureArtifactManager.getInstance(project).getArtifactIdentifier(artifact)).build();
        final DeploymentSlotConfig slotConfig = chkDeployToSlot.isSelected() ? rbtExistingSlot.isSelected() ?
                DeploymentSlotConfig.builder().newCreate(false).name(Objects.toString(cbxSlotName.getSelectedItem(), null)).build() :
                DeploymentSlotConfig.builder().newCreate(true).name(txtNewSlotName.getText())
                        .configurationSource(Objects.toString(cbxSlotConfigurationSource.getSelectedItem(), null)).build() : null;
        final WebAppConfig webAppConfig = comboBoxWebApp.getValue();
        if (webAppConfig != null) {
            webAppConfig.setDeploymentSlot(slotConfig);
        }
        return WebAppDeployRunConfigurationModel.builder()
                .webAppConfig(webAppConfig)
                .artifactConfig(artifactConfig)
                .openBrowserAfterDeployment(chkOpenBrowser.isSelected())
                .deployToRoot(chkToRoot.isSelected())
                .slotPanelVisible(slotDecorator.isExpanded())
                .build();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(comboBoxWebApp, comboBoxArtifact);
    }
}
