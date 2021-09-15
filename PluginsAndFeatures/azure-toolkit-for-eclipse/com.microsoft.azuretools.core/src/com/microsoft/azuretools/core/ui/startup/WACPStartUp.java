/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui.startup;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_INSTALL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_LOAD;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_UPGRADE;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;

import java.io.File;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.w3c.dom.Document;

import com.microsoft.azure.toolkit.ide.common.util.ParserXMLUtility;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.survey.CustomerSurveyHelper;
import com.microsoft.azuretools.core.telemetry.AppInsightsConfigurationImpl;
import com.microsoft.azuretools.core.utils.FileUtil;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;

/**
 * This class gets executed after the Workbench initializes.
 */
public class WACPStartUp implements IStartup {
    private String _hashmac = InstallationIdUtils.getHashMac();

    @Override
    public void earlyStartup() {
        initialize();

        Collection<String> obsoletePackages = Activator.getDefault().getObsoletePackages();
        if (obsoletePackages != null && !obsoletePackages.isEmpty()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Need to clean packages",
                            "You have an older version of the Azure Toolkit for Eclipse installed which "
                                    + "could not be automatically upgraded. You should uninstall all the listed "
                                    + "components manually, and reinstall from http://dl.microsoft.com/eclipse\n\n"
                                    + StringUtils.join(obsoletePackages, "\n"));

                }

            });
        }
    }

    /**
     * Method verifies presence of com.microsoft.azuretools.core folder and
     * data.xml file. It updates or creates property elements in data.xml as per
     * scenarios.
     */
    private void initialize() {
        try {
            
            String pluginInstLoc = String.format("%s%s%s", PluginUtil.pluginFolder, File.separator,
                    Messages.commonPluginID);
            final String dataFile = String.format("%s%s%s", pluginInstLoc, File.separator, Messages.dataFileName);
            boolean install = false;
            boolean upgrade = false;
            if (new File(pluginInstLoc).exists()) {
                if (new File(dataFile).exists()) {
                    String version = DataOperations.getProperty(dataFile, Messages.version);
                    if (version == null || version.isEmpty()) {
                        // proceed with setValues method as no version specified
                        setValues(dataFile);
                    } else {
                        String curVersion = Activator.getDefault().getBundle().getVersion().toString();
                        // compare version
                        if (curVersion.equalsIgnoreCase(version)) {
                            // Case of normal eclipse restart
                            // check preference-value & installation-id exists
                            // or not else copy values
                            String prefValue = DataOperations.getProperty(dataFile, Messages.prefVal);
                            String hdinsightPrefValue = DataOperations.getProperty(dataFile, Messages.hdinshgtPrefVal);
                            String instID = DataOperations.getProperty(dataFile, Messages.instID);

                            if (StringHelper.isNullOrWhiteSpace(prefValue)
                                    || StringHelper.isNullOrWhiteSpace(hdinsightPrefValue)) {
                                setValues(dataFile, StringHelper.isNullOrWhiteSpace(prefValue),
                                        StringHelper.isNullOrWhiteSpace(hdinsightPrefValue));
                            } else if (StringUtils.isEmpty(instID) || !InstallationIdUtils.isValidHashMac(instID)) {
                                upgrade = true;
                                Document doc = ParserXMLUtility.parseXMLFile(dataFile);
                                DataOperations.updatePropertyValue(doc, Messages.instID, _hashmac);
                                ParserXMLUtility.saveXMLFile(dataFile, doc);
                            }
                        } else {
                            upgrade = true;
                            // proceed with setValues method. Case of new plugin installation
                            setValues(dataFile);
                        }
                    }
                } else {
                    // copy file and proceed with setValues method
                    install = true;
                    FileUtil.copyResourceFile(Messages.dataFileEntry, dataFile);
                    setValues(dataFile);
                }
            } else {
                install = true;
                new File(pluginInstLoc).mkdir();
                FileUtil.copyResourceFile(Messages.dataFileEntry, dataFile);
                setValues(dataFile);
            }

            AppInsightsClient.setAppInsightsConfiguration(new AppInsightsConfigurationImpl());
            if (install) {
                EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_INSTALL, null, null);
                AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Install, null, true);
            }
            if (upgrade) {
                EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_UPGRADE, null, null);
                AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Upgrade, null, true);
            }
            EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_LOAD, null, null);
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Load, null, true);

            new CustomerSurveyHelper().showFeedbackNotification();
        } catch (Exception ex) {
            Activator.getDefault().log(ex.getMessage(), ex);
        }
    }

    private void setValues(final String dateFile) throws Exception {
        setValues(dateFile, true, true);
    }



    /**
     * Method updates or creates property elements in data.xml
     *
     * @param dataFile
     * @throws Exception
     */
    private void setValues(final String dataFile, final boolean isAzureToolKit, final boolean isHDInsight)
            throws Exception {
        final Document doc = ParserXMLUtility.parseXMLFile(dataFile);
        if (isAzureToolKit) {
            try {
                String recordedVersion = DataOperations.getProperty(dataFile, Messages.version);
                if (Utils.whetherUpdateTelemetryPref(recordedVersion)) {
                    DataOperations.updatePropertyValue(doc, Messages.prefVal, "true");
                }
            } catch (Exception ex){
                Activator.getDefault().log(ex.getMessage(), ex);
            }
        }

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                if (isHDInsight && Activator.getDefault().isScalaInstallationTipNeeded()) {
                    boolean isShowHDInsightTips = true;
                    HDInsightScalaHelpDlg hdInsightHelpDlg = new HDInsightScalaHelpDlg(Display.getDefault().getActiveShell());
                    if (hdInsightHelpDlg.open() == Window.CANCEL && !hdInsightHelpDlg.isShowTipsStatus()) {
                        isShowHDInsightTips = false;
                        DataOperations.updatePropertyValue(doc, Messages.hdinshgtPrefVal,
                                String.valueOf(isShowHDInsightTips));
                    }
                }
            }
        });

        DataOperations.updatePropertyValue(doc, Messages.version,
                Activator.getDefault().getBundle().getVersion().toString());
        DataOperations.updatePropertyValue(doc, Messages.instID, _hashmac);
        ParserXMLUtility.saveXMLFile(dataFile, doc);
    }
}
