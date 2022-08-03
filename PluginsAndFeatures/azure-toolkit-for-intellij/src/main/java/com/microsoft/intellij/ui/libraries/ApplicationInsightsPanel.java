/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.libraries;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.ui.AzureAbstractPanel;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXHyperlink;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class ApplicationInsightsPanel implements AzureAbstractPanel {
    private static final String DISPLAY_NAME = "Choose Application Insights Telemetry key";
    private JPanel rootPanel;
    private JCheckBox aiCheck;
    private JXHyperlink lnkAIPrivacy;
    private JLabel lblInstrumentationKey;
    private AzureComboBox<ApplicationInsight> comboInstrumentation;

    private AILibraryHandler handler;
    private Module module;

    private String webxmlPath = message("xmlPath");

    public ApplicationInsightsPanel(Module module) {
        this.module = module;
        handler = new AILibraryHandler();
        init();
    }

    private void init() {
        comboInstrumentation.reloadItems();
        initLink(lnkAIPrivacy, message("lnkAIPrivacy"), message("AIPrivacy"));
        try {
            String webXmlFilePath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, webxmlPath);
            if (new File(webXmlFilePath).exists()) {
                handler.parseWebXmlPath(webXmlFilePath);
            }
            String aiXMLFilePath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("aiXMLPath"));
            if (new File(aiXMLFilePath).exists()) {
                handler.parseAIConfXmlPath(aiXMLFilePath);
            }
        } catch (Exception ex) {
            AzurePlugin.log(message("aiParseError"));
        }
        if (isEdit()) {
            populateData();
        } else {
            if (aiCheck.isSelected()) {
                comboInstrumentation.setEnabled(true);
            } else {
                comboInstrumentation.setEnabled(false);
            }
        }
        aiCheck.addActionListener(createAiCheckListener());
    }

    private ActionListener createAiCheckListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (aiCheck.isSelected()) {
                    populateData();
                } else {
                    if (comboInstrumentation.getItemCount() > 0) {
                        comboInstrumentation.setSelectedIndex(0);
                    }
                    comboInstrumentation.setEnabled(false);
                }
            }
        };
    }

    private void initLink(JXHyperlink link, String linkText, String linkName) {
        link.setURI(URI.create(linkText));
        link.setText(linkName);
    }

    @Override
    public JComponent getPanel() {
        return rootPanel;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean doOKAction() {
        // validate
        if (aiCheck.isSelected() && (comboInstrumentation.getSelectedItem() == null)) {
            PluginUtil.displayErrorDialog(message("aiErrTitle"), message("aiInstrumentationKeyNull"));
            return false;
        } else if (!aiCheck.isSelected()) {
            // disable if exists
            try {
                handler.disableAIFilterConfiguration(true);
                handler.removeAIFilterDef();
                handler.removeAIServletContextListener();
                handler.save();
            } catch (Exception e) {
                PluginUtil.displayErrorDialog(message("aiErrTitle"), message("aiConfigRemoveError") + e.getLocalizedMessage());
                return false;
            }
        } else {
            try {
                createAIConfiguration();
                configureAzureSDK();
            } catch (Exception e) {
                PluginUtil.displayErrorDialog(message("aiErrTitle"), message("aiConfigError") + e.getLocalizedMessage());
                return false;
            }
        }
        LocalFileSystem.getInstance().findFileByPath(PluginUtil.getModulePath(module)).refresh(true, true);
        return true;
    }

    private void populateData() {
        aiCheck.setSelected(true);
        String keyFromFile = handler.getAIInstrumentationKey();
        if (StringUtils.isNotEmpty(keyFromFile)) {
            comboInstrumentation.setValue(new AzureComboBox.ItemReference<>(item -> StringUtils.equals(item.getInstrumentationKey(), keyFromFile)));
        }
        comboInstrumentation.setEnabled(true);
    }

    private boolean isEdit() {
        try {
            return handler.isAIWebFilterConfigured();
        } catch (Exception e) {
            // just return false if there is any exception
            return false;
        }
    }

    private void createAIConfiguration() throws Exception {
        handleWebXML();
        handleAppInsightsXML();
        handler.save();
    }

    private void handleWebXML() throws Exception {
        String xmlPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, webxmlPath);
        if (new File(xmlPath).exists()) {
            handler.parseWebXmlPath(xmlPath);
            handler.setAIFilterConfig();

            // workaround for application insights v2.2.0 regression:
            // An exception occurred when stop the tomcat application with application insights configured.
            // Issue was logged to track: https://github.com/Microsoft/ApplicationInsights-Java/issues/755
            handler.setAIServletContextListener();
        } else { // create web.xml
            int choice = Messages.showYesNoDialog(message("depDescMsg"), message("depDescTtl"), Messages.getQuestionIcon());
            if (choice == Messages.YES) {
                String path = createFileIfNotExists(message("depFileName"), message("depDirLoc"), message("aiWebXmlResFileLoc"));
                handler.parseWebXmlPath(path);
            } else {
                throw new Exception(": Application Insights cannot be configured without creating web.xml ");
            }
        }
    }

    private void handleAppInsightsXML() throws Exception {
        String aiXMLPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("aiXMLPath"));
        if (new File(aiXMLPath).exists()) {
            handler.parseAIConfXmlPath(aiXMLPath);
            handler.disableAIFilterConfiguration(false);
        } else { // create ApplicationInsights.xml
            String path = createFileIfNotExists(message("aiConfFileName"), message("aiConfRelDirLoc"), message("aiConfResFileLoc"));
            handler.parseAIConfXmlPath(path);
        }
        String key = comboInstrumentation.getValue().getInstrumentationKey();
        handler.setAIInstrumentationKey(key);
    }

    private void configureAzureSDK() {
        final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
        for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
            if (OrderEntryUtil.isModuleLibraryOrderEntry(orderEntry) &&
                    StringUtils.equals(AzureLibrary.APP_INSIGHTS.getName(), orderEntry.getPresentableName())) {
                return;
            }
        }

        final LibrariesContainer.LibraryLevel level = LibrariesContainer.LibraryLevel.MODULE;
        WriteAction.run(() -> {
            try {
                Library newLibrary = LibrariesContainerFactory.createContainer(modifiableModel).createLibrary(AzureLibrary.APP_INSIGHTS.getName(),
                        level, new ArrayList<OrderRoot>());
                for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
                    if (OrderEntryUtil.isModuleLibraryOrderEntry(orderEntry) &&
                            StringUtils.equals(AzureLibrary.APP_INSIGHTS.getName(), orderEntry.getPresentableName())) {
                        // todo: investigate method to set library exported
                        break;
                    }
                }
                Library.ModifiableModel newLibraryModel = newLibrary.getModifiableModel();
                AddLibraryUtility.addLibraryFiles(new File(PluginHelper.getAzureLibLocation()), newLibraryModel, AzureLibrary.APP_INSIGHTS.getFiles());

                newLibraryModel.commit();
                modifiableModel.commit();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public String createFileIfNotExists(String fileName, String relDirLocation, String resFileLoc) {
        String path = null;
        try {
            File cmpntFileLoc = new File(String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, relDirLocation));
            String cmpntFile = String.format("%s%s%s", cmpntFileLoc, File.separator, fileName);
            if (!cmpntFileLoc.exists()) {
                cmpntFileLoc.mkdirs();
            }
            AzurePlugin.copyResourceFile(resFileLoc, cmpntFile);
            path = cmpntFile;
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(message("acsErrTtl"), message("fileCrtErrMsg"), e);
        }
        return new File(path).getPath();
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    private void createUIComponents() {
        this.comboInstrumentation = new AzureComboBox<>() {
            @Nonnull
            @Override
            protected List<? extends ApplicationInsight> loadItems() {
                return Azure.az(AzureApplicationInsights.class).list().stream()
                        .flatMap((m) -> m.getApplicationInsightsModule().list().stream())
                        .sorted((first, second) -> StringUtils.compare(first.getName(), second.getName()))
                        .collect(Collectors.toList());
            }

            @Override
            protected void refreshItems() {
                Azure.az(AzureApplicationInsights.class).refresh();
                super.refreshItems();
            }

            @Override
            protected String getItemText(Object item) {
                return item instanceof ApplicationInsight ? ((ApplicationInsight) item).getName() : super.getItemText(item);
            }
        };
    }
}
