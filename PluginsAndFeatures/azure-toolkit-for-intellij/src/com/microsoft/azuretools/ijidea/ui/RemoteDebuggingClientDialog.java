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

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.management.appservice.PlatformArchitecture;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginHelper;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

public class RemoteDebuggingClientDialog extends AzureDialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(SignInWindow.class);

    private JPanel contentPane;
    private JTextField portTextField;
    private JTextField serverTextField;
    private JTextField userTextField;
    private JTextField passwordTextField;
    private JLabel serverLabel;
    private JLabel userLabel;
    private JLabel passwordLabel;
    private JLabel portLabel;
    private JButton uploadWebConfigButton;
    private JTabbedPane tabbedPane1;
    private JButton startButton;
    private JTextField ftpHostTextField;
    private JTextField ftpUsernametextField;
    private JTextField ftpPasswordTextField;
    private JLabel webSocketCurrenValueLabel;
    private JLabel platformCurrentValueLabel;
    private JXHyperlink webSocketOnLink;
    private JXHyperlink webSocketOffLink;
    private JXHyperlink platform32bitLink;
    private JXHyperlink platform64bitLink;
    private JTextField webConfigPathTextFIeld;
    private JXHyperlink webConfigOpenLink;

    private Project project;
    private WebApp webApp;

    static final String ON = "On";
    static final String OFF = "Off";
    static final String P64BITS = "64-bits";
    static final String P32BITS = "32-bits";
    static final String titleAppServiceChangeOption = "App Service Change Option";

    public RemoteDebuggingClientDialog(@Nullable Project project, @NotNull WebApp webApp) {
        super(project, true, IdeModalityType.PROJECT);
        this.project = project;
        this.webApp = webApp;
        setModal(true);
        setTitle("Remote Debugging Client Dialog");

        PublishingProfile pp = webApp.getPublishingProfile();
        final StringBuilder serverSb = new StringBuilder(webApp.defaultHostName());
        serverSb.insert(serverSb.indexOf("."), ".scm");
        serverTextField.setText(serverSb.toString());
        userTextField.setText(pp.ftpUsername().substring(pp.ftpUsername().indexOf('$')));
        passwordTextField.setText(pp.ftpPassword());

        ftpHostTextField.setText(pp.ftpUrl());
        ftpUsernametextField.setText(pp.ftpUsername());
        ftpPasswordTextField.setText(pp.ftpPassword());

        String webConfigPath = PluginHelper.getTemplateFile("remotedebug" + File.separator + "web.config");
        webConfigPathTextFIeld.setText(webConfigPath);
        webConfigOpenLink.setURI(URI.create("file:///" + webConfigPath.replaceAll("\\\\", "/" )));
        webConfigOpenLink.setText("Show");

        uploadWebConfigButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                uploadWebConfig();
            }
        });

        updateWebSocketsCurrent();
        updatePlatformCurrent();

        platform64bitLink.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("platform64bitLink action performed");
                runWithProgress(new IWorker() {
                    @Override
                    public void work(ProgressIndicator progressIndicator) throws Exception {
                        progressIndicator.setText("Enabling 64-bits...");
                        webApp.update().withPlatformArchitecture(PlatformArchitecture.X64).apply();
                    }

                    @Override
                    public void rollBack(ProgressIndicator progressIndicator) throws Exception {
                        progressIndicator.setText("Rolling back...");
                        webApp.refresh();
                    }
                }, titleAppServiceChangeOption);
                updatePlatformCurrent();
                platform64bitLink.setClicked(true);
            }

            private AbstractAction init(String name) {
                super.putValue(Action.NAME, name);
                return this;
            }
        }.init(P64BITS));

        platform32bitLink.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("platform32bitLink action performed");
                runWithProgress(new IWorker() {
                    @Override
                    public void work(ProgressIndicator progressIndicator) throws Exception {
                        progressIndicator.setText("Enabling 32-bits...");
                        webApp.update().withPlatformArchitecture(PlatformArchitecture.X86).apply();
                    }

                    @Override
                    public void rollBack(ProgressIndicator progressIndicator) throws Exception {
                        progressIndicator.setText("Rolling back...");
                        webApp.refresh();
                    }
                }, titleAppServiceChangeOption);
                updatePlatformCurrent();
                platform32bitLink.setClicked(true);
            }

            private AbstractAction init(String name) {
                super.putValue(Action.NAME, name);
                return this;
            }
        }.init(P32BITS));

        webSocketOnLink.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("webSocketOnLink action performed");
                runWithProgress(new IWorker() {
                    @Override
                    public void work(ProgressIndicator progressIndicator) throws Exception {
                        progressIndicator.setText("Enabling web sockets...");
                        webApp.update().withWebSocketsEnabled(true).apply();
                    }

                    @Override
                    public void rollBack(ProgressIndicator progressIndicator) throws Exception {
                        progressIndicator.setText("Rolling back...");
                        webApp.refresh();
                    }
                }, titleAppServiceChangeOption);
                updateWebSocketsCurrent();
                webSocketOnLink.setClicked(true);
            }

            private AbstractAction init(String name) {
                super.putValue(Action.NAME, name);
                return this;
            }
        }.init(ON));

        webSocketOffLink.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("webSocketOffLink action performed");
                runWithProgress(new IWorker() {
                    @Override
                    public void work(ProgressIndicator progressIndicator) throws Exception {
                        progressIndicator.setText("Disabling web sockets...");
                        webApp.update().withWebSocketsEnabled(false).apply();
                    }

                    @Override
                    public void rollBack(ProgressIndicator progressIndicator) throws Exception {
                        progressIndicator.setText("Rolling back...");
                        webApp.refresh();
                    }
                }, titleAppServiceChangeOption);
                updateWebSocketsCurrent();
                webSocketOffLink.setClicked(true);
            }

            private AbstractAction init(String name) {
                super.putValue(Action.NAME, name);
                return this;
            }
        }.init(OFF));

        init();
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                startClient();
            }
        });
    }

    private void updatePlatformCurrent() {
        // get platform
        String platformVal = webApp.inner().siteConfig().use32BitWorkerProcess()
                ? P32BITS
                : P64BITS;
        platformCurrentValueLabel.setText(platformVal);
    }

    private void updateWebSocketsCurrent() {
        // get web sockets
        String webSocketsVal = webApp.inner().siteConfig().webSocketsEnabled()
                ? ON
                : OFF;
        webSocketCurrenValueLabel.setText(webSocketsVal);
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{this.getCancelAction(), this.getHelpAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doHelpAction() {
        sendTelemetry(HELP_CODE);
        JXHyperlink link = new JXHyperlink();
        link.setURI(URI.create("https://github.com/Azure/azure-websites-java-remote-debugging"));
        link.doClick();
        //super.doHelpAction();
    }

    private void startClient() {
        String webAppDirPath = PluginHelper.getTemplateFile("remotedebug");
        String command = String.format("DebugSession.bat --port %s --server %s --user %s --pass %s --auto",
                portTextField.getText(),
                serverTextField.getText(),
                userTextField.getText(),
                passwordTextField.getText());
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", command);
        pb.directory(new File(webAppDirPath));
        try {
            pb.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        super.doOKAction();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "RemoteDebuggingClientDialog";
    }

    private void createUIComponents() {
        portTextField = new HintTextField("<port>");
        serverTextField = new HintTextField("<server>");
        userTextField = new HintTextField("<user>");
        passwordTextField = new HintTextField("<password>");
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (portTextField.getText().isEmpty() || !portTextField.getText().matches("^[0-9]*[0-9]$")) {
            return new ValidationInfo("Enter a valid Port value.", portTextField);
        }
        if (serverTextField.getText().isEmpty()) {
            return new ValidationInfo("The value is empty.", serverTextField);
        }
        if (userTextField.getText().isEmpty()) {
            return new ValidationInfo("The value is empty.", userTextField);
        }
        if (passwordTextField.getText().isEmpty()) {
            return new ValidationInfo("The value is empty.", passwordTextField);
        }
        return super.doValidate();
    }

    private void uploadWebConfig() {
        runWithProgress(new IWorker() {
            @Override
            public void work(ProgressIndicator progressIndicator) throws Exception {
                String webConfigPath = PluginHelper.getTemplateFile("remotedebug/web.config");
                File file = new File(webConfigPath);
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    WebAppUtils.uploadWebConfig(webApp, fis, new UpdateProgressIndicator(progressIndicator));
                } finally {
                    if (fis != null)
                        fis.close();
                }
            }

            @Override
            public void rollBack(ProgressIndicator progressIndicator) throws Exception {
                // do nothing
            }
        }, titleAppServiceChangeOption);
    }

    interface IWorker {
        void work(ProgressIndicator progressIndicator) throws Exception;

        void rollBack(ProgressIndicator progressIndicator) throws Exception;
    }

    protected void runWithProgress(IWorker worker, String title) {
        AzureTaskRunner.getInstance().runInModal(new AzureTask(project, title + " Progress", true, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setIndeterminate(true);
            try {
                worker.work(progressIndicator);
            } catch (Exception ex) {
                ex.printStackTrace();
                //LOGGER.error("work@IWorker@run@ProgressManager@runWithProgress@RemoteDebuggingClientDialog", ex);
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ErrorWindow.show(project, ex.getMessage(), title + "Error");
                    }
                });
                try {
                    worker.rollBack(progressIndicator);
                } catch (Exception ex1) {
                    LOGGER.error("rollback@IWorker@run@ProgressManager@runWithProgress@RemoteDebuggingClientDialog", ex1);
                }
            }
        }));
    }

}
