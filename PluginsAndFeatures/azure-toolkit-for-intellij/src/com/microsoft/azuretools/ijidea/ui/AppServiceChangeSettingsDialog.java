/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator;
import com.microsoft.azuretools.utils.IProgressIndicator;
import com.microsoft.azuretools.utils.WebAppUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static com.microsoft.azuretools.utils.WebAppUtils.WebAppDetails;

/**
 * Created by vlashch on 1/30/17.
 */
public class AppServiceChangeSettingsDialog extends AppServiceCreateDialog {
    private static final Logger LOGGER = Logger.getInstance(AppServiceChangeSettingsDialog.class);

    public static AppServiceChangeSettingsDialog go(WebAppDetails wad, Project project){
        AppServiceChangeSettingsDialog d = new AppServiceChangeSettingsDialog(project, wad);
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }
        return null;
    }

    protected AppServiceChangeSettingsDialog(Project project, WebAppDetails wad) {
        super(project);

        setTitle("Change App Service Settings");

        setOKButtonText("Change");

        webApp = wad.webApp;
        textFieldWebappName.setText(wad.webApp.name());
        textFieldWebappName.setEnabled(false);

        DefaultComboBoxModel<WebContainer> wcModel = (DefaultComboBoxModel<WebContainer>)comboBoxWebContainer.getModel();
        if (wad.webApp.javaVersion() != JavaVersion.OFF) {
            wcModel.setSelectedItem(WebContainer.fromString(wad.webApp.javaContainer() + " " + wad.webApp.javaContainerVersion()));
        }

        comboBoxSubscription.setEnabled(false);

        DefaultComboBoxModel<ResourceGroupAdapter> rgModel = (DefaultComboBoxModel<ResourceGroupAdapter>)comboBoxResourceGroup.getModel();
        rgModel.setSelectedItem(new ResourceGroupAdapter(wad.resourceGroup));
        for (Component c : panelResourceGroupUseExisting.getComponents()) {
            c.setEnabled(false);
        }

        for (Component c : panelResourceGroup.getComponents()) {
            c.setEnabled(false);
        }

        DefaultComboBoxModel<AppServicePlanAdapter> aspModel = (DefaultComboBoxModel<AppServicePlanAdapter> )comboBoxAppServicePlan.getModel();
        aspModel.setSelectedItem(new AppServicePlanAdapter(wad.appServicePlan));
        //comboBoxAppServicePlan.setEnabled(false);
        for (Component c : panelAppServiceUseExisting.getComponents()) {
            c.setEnabled(false);
        }

        for (Component c : panelAppServicePlan.getComponents()) {
            c.setEnabled(false);
        }
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {

        model.collectData();

        ValidationInfo res = validateJdkTab();
        if (res != null) return res;

        return super.superDoValidate();
    }

    @Override
    protected void doOKAction() {
        ProgressManager.getInstance().run(new Task.Modal(null,"Changing App Service Settings...", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);
                    webApp = editAppService(webApp, new UpdateProgressIndicator(progressIndicator));
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            superDoOKAction();
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // TODO: show error message
                    LOGGER.error("doOKAction :: Task.Modal", ex);
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ErrorWindow.show(project, ex.getMessage(), "Create App Service Error");
                        }
                    });
                }
            }
        });
    }

    protected WebApp editAppService(WebApp webApp, IProgressIndicator progressIndicator) throws Exception {
        if (model.jdkDownloadUrl != null ) {
            progressIndicator.setText("Turning App Service into .Net based...");

            //webApp.update().withNetFrameworkVersion(NetFrameworkVersion.V4_6).apply();
            //progressIndicator.setText("Deploying custom jdk...");
            //WebAppUtils.deployCustomJdk(webApp, model.jdkDownloadUrl, model.webContainer, progressIndicator);
        } else {
            FTPClient ftpClient = WebAppUtils.getFtpConnection(webApp.getPublishingProfile());
            progressIndicator.setText("Deleting custom jdk artifacts, if any (takes a while)...");
            WebAppUtils.removeCustomJdkArtifacts(ftpClient, progressIndicator);
            // TODO: make cancelable
            WebAppUtils.removeCustomJdkArtifacts(WebAppUtils.getFtpConnection(webApp.getPublishingProfile()), progressIndicator);
            progressIndicator.setText("Applying changes...");
            webApp.update().withJavaVersion(JavaVersion.JAVA_8_NEWEST).withWebContainer(model.webContainer).apply();
        }

        return webApp;
    }
}
