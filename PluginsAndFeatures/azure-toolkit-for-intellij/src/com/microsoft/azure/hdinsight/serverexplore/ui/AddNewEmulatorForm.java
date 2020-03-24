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

package com.microsoft.azure.hdinsight.serverexplore.ui;

import com.google.common.util.concurrent.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddNewEmulatorForm extends DialogWrapper {
    static final com.intellij.openapi.diagnostic.Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance("#com.microsoft.intellij.AzurePlugin");
    private JPanel addNewEmulatorPanel;
    private JTextField clusterNameField;
    private JTextField userNameField;
    private JPasswordField passwordField;
    private JTextField livyEndpointField;
    private JPanel buttonPanel;
    private JButton OkButton;
    private JButton cancelButton;
    private JTextField errorMessageField;
    private JTextField sshEndpointField;
    private JTextField sparkHistoryEndpointField;
    private JTextField ambariEndpointField;
    private JTextArea emulatorSetupText;
    private JPanel emulatorLogPanel;
    private JScrollPane emulatorScrollPanel;

    private HDInsightRootModule hdInsightModule;
    private Project project;
    private String errorMessage;
    private String clusterName;
    private String userName;
    private String password;
    private String livyEndpoint;
    private String sshEndpoint;
    private String sparkHistoryEndpoint;
    private String ambariEndpoint;
    private StringBuilder emulatorSetupLog;
    private String host;
    private int sshPort;
    private Session session;
    private JSch jsch;
    private ChannelExec channel;
    private ChannelSftp channelSftp;
    private Properties config;
    private boolean isCarryOnNextStep;

    public AddNewEmulatorForm(final Project project, HDInsightRootModule hdInsightModule) {
        super(project, true);
        init();
        this.project = project;
        this.hdInsightModule = hdInsightModule;

        this.setTitle("Link a New Emulator");

        errorMessageField.setBackground(this.addNewEmulatorPanel.getBackground());
        errorMessageField.setBorder(BorderFactory.createEmptyBorder());

        this.setModal(true);
        addActionListener();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return clusterNameField;
    }

    private void addActionListener() {
        OkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (AddNewEmulatorForm.class){
                    preEmulatorSetupCheck();
                    if(isCarryOnNextStep && !checkEmulatorSetup()) {
                        int dialogButton = JOptionPane.YES_NO_OPTION;
                        int dialogResult = JOptionPane.showConfirmDialog(null, "We detect this emulator has not been configured yet. Would you like to set up emulator first (takes about 10 min)?","Warning",dialogButton);
                        if(dialogResult == JOptionPane.YES_OPTION){
                            emulatorLogPanel.setVisible(true);
                            setEnable(false);
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            ListeningExecutorService executorService = MoreExecutors.listeningDecorator(executor);
                            ListenableFuture<Boolean> futureTask = executorService.submit(new Callable<Boolean>()
                            {
                                @Override
                                public Boolean call() throws Exception {
                                    Boolean ready = true;
                                    session = jsch.getSession(userName, host, sshPort);
                                    session.setPassword(password);
                                    session.setConfig(config);
                                    session.connect();
                                    channelSftp = (ChannelSftp) session.openChannel("sftp");
                                    channelSftp.connect();
                                    File zipFile = StreamUtil.getResourceFile(CommonConst.EmualtorPath + CommonConst.EmulatorArchieveFileName);
                                    File scriptFile = StreamUtil.getResourceFile(CommonConst.EmualtorPath + CommonConst.EmulatorSetupScriptFileName);
                                    channelSftp.put(new FileInputStream(zipFile), CommonConst.EmulatorArchieveFileName);
                                    channelSftp.put(new FileInputStream(scriptFile), CommonConst.EmulatorSetupScriptFileName);

                                    channel = (ChannelExec) session.openChannel("exec");
                                    BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                                    channel.setCommand("perl -pi -e 's/\\r\\n/\\n/' setup.sh;chmod +x setup.sh;./setup.sh");
                                    channel.connect();
                                    String result = null;
                                    while ((result = in.readLine()) != null) {
                                        if (result.length() != 0) {
                                            emulatorSetupLog.append(result + "\n");
                                            emulatorSetupText.setText(emulatorSetupLog.toString());
                                        }
                                    }

                                    ready &= channel.getExitStatus() == 0;
                                    channel.disconnect();
                                    channelSftp.disconnect();
                                    session.disconnect();
                                    return ready;
                                }
                            });
                            Futures.addCallback(futureTask, new FutureCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean b) {
                                    isCarryOnNextStep = b;
                                    setEnable(true);
                                    postEmulatorSetupCheck();
                                    showEmulatorSetup();
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    errorMessage = throwable.getMessage();
                                    isCarryOnNextStep = false;
                                    setEnable(true);
                                    showEmulatorSetup();
                                }
                            }, MoreExecutors.directExecutor());
                        } else {
                            errorMessage = "Local emulator is not set up yet.";
                            isCarryOnNextStep = false;
                            showEmulatorSetup();
                        }
                    } else {
                        postEmulatorSetupCheck();
                        showEmulatorSetup();
                    }
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close(DialogWrapper.CANCEL_EXIT_CODE, true);
            }
        });
    }

    private void preEmulatorSetupCheck(){
        isCarryOnNextStep = true;
        errorMessage = null;
        emulatorSetupLog = new StringBuilder();
        errorMessageField.setText("");
        emulatorLogPanel.setVisible(false);

        clusterName = clusterNameField.getText().trim();
        userName = userNameField.getText().trim();
        password = String.valueOf(passwordField.getPassword());
        livyEndpoint = livyEndpointField.getText().trim().replaceAll("/+$","");
        sshEndpoint = sshEndpointField.getText().trim().replaceAll("/+$","");
        sparkHistoryEndpoint = sparkHistoryEndpointField.getText().trim().replaceAll("/+$","");
        ambariEndpoint = ambariEndpointField.getText().trim().replaceAll("/+$","");

        AppInsightsClient.create(HDInsightBundle.message("HDInsightCreateLocalEmulator"), null);
        EventUtil.logEvent(EventType.info, TelemetryConstants.HDINSIGHT,
            HDInsightBundle.message("HDInsightCreateLocalEmulator"), null);
        try {
            host = new URI(sshEndpoint).getHost();
            sshPort = new URI(sshEndpoint).getPort();
            jsch = new JSch();
            config = new Properties();
            config.put("StrictHostKeyChecking", "no");
        } catch (Exception exception) {
            errorMessage = exception.getMessage();
            isCarryOnNextStep = false;
        }

        if (isCarryOnNextStep && StringHelper.isNullOrWhiteSpace(clusterName) || StringHelper.isNullOrWhiteSpace(sshEndpoint)
                || StringHelper.isNullOrWhiteSpace(livyEndpoint) || StringHelper.isNullOrWhiteSpace(userName) || StringHelper.isNullOrWhiteSpace(password)
                || StringHelper.isNullOrWhiteSpace(sparkHistoryEndpoint) || StringHelper.isNullOrWhiteSpace(ambariEndpoint)) {
            errorMessage = "Cluster Name, Endpoint fields, User Name, or Password shouldn't be empty";
            isCarryOnNextStep = false;
        } else if(isCarryOnNextStep && ClusterManagerEx.getInstance().isEmulatorClusterExist(clusterName)) {
            errorMessage = "Cluster Name already exist in current list";
            isCarryOnNextStep = false;
        } else if(isCarryOnNextStep && !checkSshEndpoint()) {
            errorMessage = "Could not ssh to emulator, please check username, password and ssh port.";
            isCarryOnNextStep = false;
        }
    }

    private void postEmulatorSetupCheck(){
        if (isCarryOnNextStep && !checkLivyEndpoint()) {
            errorMessage = "Could not connect to livy, please check your endpoint";
            isCarryOnNextStep = false;
        } else if(isCarryOnNextStep && !checkSparkHistoryEndpoint()) {
            errorMessage = "Could not connect to spark history server, please check your endpoint.";
            isCarryOnNextStep = false;
        } else if(isCarryOnNextStep && !checkAmbariEndpoint()) {
            errorMessage = "Could not connect to ambari server, please check your endpoint.";
            isCarryOnNextStep = false;
        }
    }

    private void showEmulatorSetup(){
        if (isCarryOnNextStep) {
            EmulatorClusterDetail emulatorClusterDetail = new EmulatorClusterDetail(clusterName, userName, password,livyEndpoint, sshEndpoint, sparkHistoryEndpoint, ambariEndpoint);
            ClusterManagerEx.getInstance().addEmulatorCluster(emulatorClusterDetail);
            hdInsightModule.load(false);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    close(DialogWrapper.OK_EXIT_CODE, true);
                }
            });
        } else {
            errorMessageField.setText(errorMessage);
        }
    }

    private boolean checkEmulatorSetup() {
        boolean isEmulatorSetup = false;
        try{
            session = jsch.getSession(userName, host, sshPort);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();
            channel=(ChannelExec) session.openChannel("exec");
            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.setCommand(String.format("curl --fail --silent -I %s | grep HTTP/1.1 | awk {'print $2'};", livyEndpoint));
            channel.connect();
            String result = in.readLine();
            if(result != null && result.equals("200")){
                isEmulatorSetup = true;
            }

            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            return false;
        }

        return isEmulatorSetup;
    }

    private void setEnable(boolean enable){
        clusterNameField.setEnabled(enable);
        userNameField.setEnabled(enable);
        passwordField.setEnabled(enable);
        livyEndpointField.setEnabled(enable);
        OkButton.setEnabled(enable);
        cancelButton.setEnabled(enable);
        sshEndpointField.setEnabled(enable);
        sparkHistoryEndpointField.setEnabled(enable);
        ambariEndpointField.setEnabled(enable);
    }

    private boolean checkSshEndpoint() {
        try {
            session = jsch.getSession(userName, host, sshPort);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();
            session.disconnect();
        } catch ( Exception e) {
            return false;
        }

        return true;
    }

    private boolean checkLivyEndpoint() {
        SparkBatchSubmission.getInstance().setUsernamePasswordCredential(userName, password);
        HttpResponse httpResponse = null;

        try {
            httpResponse = SparkBatchSubmission.getInstance().getAllBatchesSparkJobs(livyEndpoint + "/batches");
        } catch (Exception e) {
            return false;
        }

        return httpResponse.getCode() == 201 || httpResponse.getCode() == 200;
    }

    private boolean checkSparkHistoryEndpoint() {
        SparkBatchSubmission.getInstance().setUsernamePasswordCredential(userName, password);
        HttpResponse httpResponse = null;

        try {
            httpResponse = SparkBatchSubmission.getInstance().getHttpResponseViaGet(sparkHistoryEndpoint + "/api/v1/applications");
        } catch (Exception e) {
            return false;
        }
        return httpResponse.getCode() == 201 || httpResponse.getCode() == 200 || httpResponse.getCode() == 500;
    }

    private boolean checkAmbariEndpoint() {
        SparkBatchSubmission.getInstance().setUsernamePasswordCredential("admin", "admin");
        HttpResponse httpResponse = null;

        try {
            httpResponse = SparkBatchSubmission.getInstance().getHttpResponseViaGet(ambariEndpoint);
        } catch (Exception e) {
            return false;
        }

        return httpResponse.getCode() == 201 || httpResponse.getCode() == 200;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[0];
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[0];
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return addNewEmulatorPanel;
    }
}
