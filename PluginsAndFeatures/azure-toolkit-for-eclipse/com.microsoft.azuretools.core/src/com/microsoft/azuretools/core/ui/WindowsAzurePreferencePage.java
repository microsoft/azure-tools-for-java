/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_INSTALL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_UPGRADE;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.TELEMETRY_ALLOW;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.TELEMETRY_DENY;

import java.io.File;
import java.net.URL;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Document;

import com.microsoft.azure.toolkit.ide.common.util.ParserXMLUtility;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.utils.FileUtil;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.TelemetryUtils;

/**
 * Class creates azure preference page.
 */
public class WindowsAzurePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    Button btnPreference;
    String pluginInstLoc = String.format("%s%s%s", PluginUtil.pluginFolder, File.separator, Messages.commonPluginID);
    String dataFile = String.format("%s%s%s", pluginInstLoc, File.separator, Messages.dataFileName);

    @Override
    public String getTitle() {
        String prefState = Activator.getPrefState();
        if (prefState.isEmpty()) {
            if (new File(pluginInstLoc).exists() && new File(dataFile).exists()) {
                String prefValue = DataOperations.getProperty(dataFile, Messages.prefVal);
                if (prefValue != null && !prefValue.isEmpty()) {
                    if (prefValue.equals("true")) {
                        btnPreference.setSelection(true);
                    }
                }
            }
        } else {
            // if changes are not saved yet (i.e. just navigated to other preference pages)
            // then populate temporary value
            if (prefState.equalsIgnoreCase("true")) {
                btnPreference.setSelection(true);
            } else {
                btnPreference.setSelection(false);
            }
        }
        return super.getTitle();
    }

    @Override
    public void init(IWorkbench arg0) {
    }

    @Override
    protected Control createContents(Composite parent) {
        noDefaultAndApplyButton();
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);
        btnPreference = new Button(container, SWT.CHECK);
        btnPreference.setText(Messages.preferenceMsg);
        Link urlLink = new Link(container, SWT.LEFT);
        urlLink.setText(Messages.preferenceLinkMsg);
        urlLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
                } catch (Exception ex) {
                    Activator.getDefault().log(Messages.lnkOpenErrMsg, ex);
                }
            }
        });
        return container;
    }

    @Override
    public boolean okToLeave() {
        Activator.setPrefState(String.valueOf(btnPreference.getSelection()));
        return super.okToLeave();
    }

    @Override
    public boolean performCancel() {
        Activator.setPrefState("");
        return super.performCancel();
    }

    @Override
    public boolean performOk() {
        boolean isSet = true;
        try {
            if (new File(pluginInstLoc).exists()) {
                if (new File(dataFile).exists()) {
                    Document doc = ParserXMLUtility.parseXMLFile(dataFile);
                    String oldPrefVal = DataOperations.getProperty(dataFile, Messages.prefVal);
                    DataOperations.updatePropertyValue(doc, Messages.prefVal,
                            String.valueOf(btnPreference.getSelection()));

                    final String version = DataOperations.getProperty(dataFile, Messages.version);
                    final String newVersion = Activator.getDefault().getBundle().getVersion().toString();
                    if (version == null || version.isEmpty()) {
                        DataOperations.updatePropertyValue(doc, Messages.version, newVersion);
                    } else if (!newVersion.equalsIgnoreCase(version)) {
                        DataOperations.updatePropertyValue(doc, Messages.version, newVersion);
                        AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Upgrade, null, true);
                        EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_UPGRADE, null, null);
                    }

                    String instID = DataOperations.getProperty(dataFile, Messages.instID);
                    if (instID == null || instID.isEmpty() || !InstallationIdUtils.isValidHashMac(instID)) {
                        DataOperations.updatePropertyValue(doc, Messages.instID, InstallationIdUtils.getHashMac());
                        AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Install, null, true);
                        EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_INSTALL, null, null);
                    }
                    ParserXMLUtility.saveXMLFile(dataFile, doc);
                    // Its necessary to call application insights custom create
                    // event after saving data.xml
                    final boolean acceptTelemetry = btnPreference.getSelection();
                    if (StringUtils.isNullOrEmpty(oldPrefVal) || Boolean.valueOf(oldPrefVal) != acceptTelemetry) {
                         // Boolean.valueOf(oldPrefVal) != acceptTelemetry means
                         // user changes his mind.
                         // Either from Agree to Deny, or from Deny to Agree.
                         final String action = acceptTelemetry ? AppInsightsConstants.Allow : AppInsightsConstants.Deny;
                         AppInsightsClient.createByType(AppInsightsClient.EventType.Telemetry, "", action, null, true);
                        EventUtil.logEvent(EventType.info, SYSTEM, acceptTelemetry ? TELEMETRY_ALLOW : TELEMETRY_DENY,
                            null, null);
                    }
                } else {
                    FileUtil.copyResourceFile(Messages.dataFileEntry, dataFile);
                    setValues(dataFile);
                }
            } else {
                new File(pluginInstLoc).mkdir();
                FileUtil.copyResourceFile(Messages.dataFileEntry, dataFile);
                setValues(dataFile);
            }
            String userAgent = String.format(Activator.USER_AGENT, Azure.az().config().getVersion(),
                    Azure.az().config().getMachineId());
            CommonSettings.setUserAgent(userAgent);
        } catch (Exception ex) {
            isSet = false;
            Activator.getDefault().log(ex.getMessage(), ex);
        }
        if (isSet) {
            // forget temporary values once OK button has been pressed.
            Activator.setPrefState("");
            return super.performOk();
        } else {
            PluginUtil.displayErrorDialog(getShell(), Messages.err, Messages.prefSaveErMsg);
            return false;
        }
    }

    /**
     * Method updates or creates property elements in data.xml
     *
     * @param dataFile
     * @throws Exception
     */
    private void setValues(String dataFile) throws Exception {
        Document doc = ParserXMLUtility.parseXMLFile(dataFile);
        DataOperations.updatePropertyValue(doc, Messages.version,
                Activator.getDefault().getBundle().getVersion().toString());
        DataOperations.updatePropertyValue(doc, Messages.instID, InstallationIdUtils.getHashMac());
        DataOperations.updatePropertyValue(doc, Messages.prefVal, String.valueOf(btnPreference.getSelection()));
        ParserXMLUtility.saveXMLFile(dataFile, doc);
    }
}
