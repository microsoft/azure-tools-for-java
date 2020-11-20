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

package com.microsoft.intellij.runner.webapp.webappconfig.slimui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppComboBox;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppComboBoxModel;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.webapp.Constants;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppConfiguration;
import com.microsoft.intellij.ui.components.AzureArtifact;
import com.microsoft.intellij.ui.components.AzureArtifactManager;
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

public class WebAppSlimSettingPanel extends AzureSettingPanel<WebAppConfiguration> implements WebAppDeployMvpViewSlim {
    private static final String[] FILE_NAME_EXT = {"war", "jar", "ear"};
    private static final String DEPLOYMENT_SLOT = "Deployment Slot";
    private static final String DEFAULT_SLOT_NAME = "slot-%s";
    private static final String DEPLOYMENT_SLOT_HOVER = "Deployment slots are live apps with their own hostnames. App" +
            " content and configurations elements can be swapped between two deployment slots, including the production " +
            "slot.";

    private WebAppDeployViewPresenterSlim presenter = null;

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
    private HideableDecorator slotDecorator;

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

        Icon informationIcon = AllIcons.General.ContextHelp;
        btnSlotHover.setIcon(informationIcon);
        btnSlotHover.setHorizontalAlignment(SwingConstants.CENTER);
        btnSlotHover.setPreferredSize(new Dimension(informationIcon.getIconWidth(), informationIcon.getIconHeight()));
        btnSlotHover.setToolTipText(DEPLOYMENT_SLOT_HOVER);
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
        presenter.onDetachView();
    }

    @Override
    public synchronized void fillDeploymentSlots(List<DeploymentSlot> slotList, @NotNull final WebAppComboBoxModel selectedWebApp) {
        final String defaultSlot = (String) cbxSlotName.getSelectedItem();
        final String defaultConfigurationSource = (String) cbxSlotConfigurationSource.getSelectedItem();
        cbxSlotName.removeAllItems();
        cbxSlotConfigurationSource.removeAllItems();
        cbxSlotConfigurationSource.addItem(Constants.DO_NOT_CLONE_SLOT_CONFIGURATION);
        cbxSlotConfigurationSource.addItem(selectedWebApp.getAppName());
        slotList.stream().filter(slot -> slot != null).forEach(slot -> {
            cbxSlotName.addItem(slot.name());
            cbxSlotConfigurationSource.addItem(slot.name());
        });
        setComboBoxDefaultValue(cbxSlotName, defaultSlot);
        setComboBoxDefaultValue(cbxSlotConfigurationSource, defaultConfigurationSource);
        boolean existDeploymentSlot = slotList.size() > 0;
        lblNewSlot.setVisible(!existDeploymentSlot);
        cbxSlotName.setVisible(existDeploymentSlot);
    }

    private void setComboBoxDefaultValue(JComboBox comboBox, Object value) {
        Object defaultItem = UIUtils.listComboBoxItems(comboBox).stream().filter(item -> item.equals(value)).findFirst().orElse(null);
        if (defaultItem != null) {
            comboBox.setSelectedItem(value);
        }
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlRoot;
    }

    @NotNull
    @Override
    protected JComboBox<Artifact> getCbArtifact() {
        return new JComboBox<>();
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return new JLabel();
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return new JComboBox<>();
    }

    @NotNull
    @Override
    protected JLabel getLblMavenProject() {
        return new JLabel();
    }

    @Override
    protected void resetFromConfig(@NotNull WebAppConfiguration configuration) {
        final WebAppComboBoxModel configurationModel = new WebAppComboBoxModel(configuration.getModel());
        if (StringUtils.isAllEmpty(configurationModel.getAppName(), configurationModel.getResourceId())) {
            comboBoxWebApp.refreshItems();
        } else {
            comboBoxWebApp.refreshItemsWithDefaultValue(configurationModel);
        }
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
            final boolean useNewDeploymentSlot = Comparing.equal(configuration.getSlotName(),
                                                                 Constants.CREATE_NEW_SLOT);
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
        syncBeforeRunTasks(comboBoxArtifact.getValue(), configuration);
    }

    private boolean isAbleToDeployToRoot(final AzureArtifact azureArtifact) {
        final WebAppComboBoxModel selectedWebApp = getSelectedWebApp();
        if (selectedWebApp == null || azureArtifact == null) {
            return false;
        }
        final String runtime = selectedWebApp.getRuntime();
        final String packaging = AzureArtifactManager.getInstance(project).getPackaging(azureArtifact);
        final boolean isDeployingWar = StringUtils.equalsAnyIgnoreCase(packaging, MavenConstants.TYPE_WAR, "ear");
        return isDeployingWar && StringUtils.containsIgnoreCase(runtime, "tomcat") || StringUtils.containsIgnoreCase(runtime, "jboss");
    }

    private void toggleSlotPanel(boolean slot) {
        boolean isDeployToSlot = slot && (getSelectedWebApp() != null);
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
        lblNewSlot = new HyperlinkLabel("No available deployment slot, click to create a new one");
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
