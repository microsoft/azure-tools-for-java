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
package com.microsoft.intellij.ui.libraries;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.ModuleLibraryOrderEntryImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.ui.AppInsightsMngmtPanel;
import com.microsoft.intellij.ui.AzureAbstractPanel;
import com.microsoft.intellij.ui.components.DefaultDialogWrapper;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ApplicationInsightsPanel implements AzureAbstractPanel {
    private static final String DISPLAY_NAME = "Choose Application Insights Telemetry key";
    private JPanel rootPanel;
    private JCheckBox aiCheck;
    private JXHyperlink lnkInstrumentationKey;
    private JXHyperlink lnkAIPrivacy;
    private JLabel lblInstrumentationKey;
    private JComboBox comboInstrumentation;

    private AILibraryHandler handler;
    private Module module;

    private String webxmlPath = message("xmlPath");

    public ApplicationInsightsPanel(Module module) {
        this.module = module;
        handler = new AILibraryHandler();
        init();
    }

    private void init() {
        lnkInstrumentationKey.setAction(createApplicationInsightsAction());
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
        setData();
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

    private Action createApplicationInsightsAction() {
        return new ApplicationInsightsAction();
    }

    private class ApplicationInsightsAction extends AbstractAction {
        private ApplicationInsightsAction() {
            super("Application Insights...");
        }

        public void actionPerformed(ActionEvent e) {
            String oldName = (String) comboInstrumentation.getSelectedItem();
            Project project = module.getProject();
            AppInsightsMngmtPanel appInsightsMngmtPanel = new AppInsightsMngmtPanel(project);
            appInsightsMngmtPanel.init();
            final DefaultDialogWrapper dialog = new DefaultDialogWrapper(project, appInsightsMngmtPanel);
            dialog.show();
            setData();
            List<String> list = Arrays.asList(ApplicationInsightsResourceRegistry.getResourcesNamesToDisplay());
            // check user has not removed all entries from registry.
            if (list.size() > 0) {
                if (dialog.isOK()) {
                    String newKey = dialog.getSelectedValue();
                    if (newKey != null && ApplicationInsightsResourceRegistry.getResourceIndexAsPerKey(newKey) >= 0) {
                        comboInstrumentation.setSelectedItem(list.get(ApplicationInsightsResourceRegistry.getResourceIndexAsPerKey(newKey)));
                    } else if (list.contains(oldName)) {
                        comboInstrumentation.setSelectedItem(oldName);
                    }
                } else if (list.contains(oldName)) {
                    // if oldName is not present then its already set to first entry via setData method
                    comboInstrumentation.setSelectedItem(oldName);
                }
            }
        }
    }

    private void setData() {
        AppInsightsMngmtPanel.refreshDataForDialog();
        comboInstrumentation.removeAllItems();
        String[] array = ApplicationInsightsResourceRegistry.getResourcesNamesToDisplay();
        if (array.length > 0) {
            comboInstrumentation.setModel(new DefaultComboBoxModel(array));
            comboInstrumentation.setSelectedItem(array[0]);
        }
    }

    private ActionListener createAiCheckListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (aiCheck.isSelected()) {
                    setData();
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
        if (aiCheck.isSelected() && (comboInstrumentation.getSelectedItem() == null || ((String) comboInstrumentation.getSelectedItem()).isEmpty())) {
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
        int index = -1;
        if (keyFromFile != null && !keyFromFile.isEmpty()) {
            index = ApplicationInsightsResourceRegistry.getResourceIndexAsPerKey(keyFromFile);
        }
        if (index >= 0) {
            String[] array = ApplicationInsightsResourceRegistry.getResourcesNamesToDisplay();
            comboInstrumentation.setSelectedItem(array[index]);
        } else {
            /*
             * User has specifically removed single entry or all entries from the registry,
             * which we added during eclipse start up
             * hence it does not make sense to put an entry again. Just leave as it is.
             * If registry is non empty, then value will be set to 1st entry.
             * If registry is empty, then combo box will be empty.
             */
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
        String key = (String) comboInstrumentation.getSelectedItem();
        if (key != null && key.length() > 0) {
            int index = comboInstrumentation.getSelectedIndex();
            if (index >= 0) {
                handler.setAIInstrumentationKey(ApplicationInsightsResourceRegistry.getKeyAsPerIndex(index));
            }
        }
    }

    private void configureAzureSDK() {
        final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
        for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
            if (orderEntry instanceof ModuleLibraryOrderEntryImpl
                    && AzureLibrary.APP_INSIGHTS.getName().equals(((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName())) {
                return;
            }
        }

        final LibrariesContainer.LibraryLevel level = LibrariesContainer.LibraryLevel.MODULE;
        AccessToken token = WriteAction.start();
        try {
            Library newLibrary = LibrariesContainerFactory.createContainer(modifiableModel).createLibrary(AzureLibrary.APP_INSIGHTS.getName(), level, new ArrayList<OrderRoot>());
            for (OrderEntry orderEntry : modifiableModel.getOrderEntries()) {
                if (orderEntry instanceof ModuleLibraryOrderEntryImpl
                        && AzureLibrary.APP_INSIGHTS.getName().equals(((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName())) {
                    ((ModuleLibraryOrderEntryImpl) orderEntry).setExported(true);
                    break;
                }
            }
            Library.ModifiableModel newLibraryModel = newLibrary.getModifiableModel();
            AddLibraryUtility.addLibraryFiles(new File(PluginHelper.getAzureLibLocation()), newLibraryModel, AzureLibrary.APP_INSIGHTS.getFiles());

            newLibraryModel.commit();
            modifiableModel.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            token.finish();
        }
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
}
