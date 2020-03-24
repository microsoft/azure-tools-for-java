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

package com.microsoft.azuretools.azureexplorer.forms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.jcraft.jsch.*;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.telemetry.AppInsightsClient;

public class AddNewEmulatorForm extends AzureTitleAreaDialogWrapper {

    private HDInsightRootModule hdInsightModule;
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

    private Text clusterNameField;
    private Text userNameField;
    private Text passwordField;
    private Text livyEndpointField;
    private Label errorMessageField;
    private Text sshEndpointField;
    private Text sparkHistoryEndpointField;
    private Text ambariEndpointField;
    private Text emulatorSetupText;
    private Label setupLogLabel;

    public AddNewEmulatorForm(Shell parentShell, HDInsightRootModule module) {
        super(parentShell);
        this.hdInsightModule = module;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Link New HDInsight Emulator");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Link New HDInsight Emulator");
        setMessage("Please enter HDInsight Emulator details");
        setHelpAvailable(false);
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
        gridData.widthHint = 350;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        container.setLayoutData(gridData);

        Label clusterNameLabel = new Label(container, SWT.LEFT);
        clusterNameLabel.setText("Emulator Name:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        clusterNameLabel.setLayoutData(gridData);
        clusterNameField = new Text(container, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        clusterNameField.setLayoutData(gridData);

        Label livyEndpointLabel = new Label(container, SWT.LEFT);
        livyEndpointLabel.setText("Livy Endpoint:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        livyEndpointLabel.setLayoutData(gridData);
        livyEndpointField = new Text(container, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        livyEndpointField.setLayoutData(gridData);

        Label sshEndpointLabel = new Label(container, SWT.LEFT);
        sshEndpointLabel.setText("Ssh Endpoint:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        sshEndpointLabel.setLayoutData(gridData);
        sshEndpointField = new Text(container, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        sshEndpointField.setLayoutData(gridData);

        Label ambariEndpointLabel = new Label(container, SWT.LEFT);
        ambariEndpointLabel.setText("Ambari Endpoint:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        ambariEndpointLabel.setLayoutData(gridData);
        ambariEndpointField = new Text(container, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        ambariEndpointField.setLayoutData(gridData);

        Label sparkHistoryEndpointLabel = new Label(container, SWT.LEFT);
        sparkHistoryEndpointLabel.setText("Spark History Endpoint:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        sparkHistoryEndpointLabel.setLayoutData(gridData);
        sparkHistoryEndpointField = new Text(container, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        sparkHistoryEndpointField.setLayoutData(gridData);


        Label userNameLabel = new Label(container, SWT.LEFT);
        userNameLabel.setText("User Name:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        userNameLabel.setLayoutData(gridData);
        userNameField = new Text(container, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        userNameField.setLayoutData(gridData);

        Label passwordLabel = new Label(container, SWT.LEFT);
        passwordLabel.setText("Password:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        passwordLabel.setLayoutData(gridData);
        passwordField = new Text(container, SWT.PASSWORD | SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        passwordField.setLayoutData(gridData);

        setupLogLabel = new Label(container, SWT.LEFT);
        setupLogLabel.setText("Setup Log:");
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        setupLogLabel.setLayoutData(gridData);
        emulatorSetupText = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        emulatorSetupText.setLayoutData(gridData);

        errorMessageField = new Label(container, SWT.FILL);
        GridData gridData2 = new GridData(GridData.VERTICAL_ALIGN_END);
        gridData2.horizontalSpan = 2;
        gridData2.horizontalAlignment = GridData.FILL;
        errorMessageField.setLayoutData(gridData2);
        errorMessageField.setVisible(false);

        emulatorSetupText.setVisible(false);
        setupLogLabel.setVisible(false);
        clusterNameField.setText("emualtor");
        livyEndpointField.setText("http://127.0.0.1:8998");
        sshEndpointField.setText("http://127.0.0.1:2222");
        ambariEndpointField.setText("http://127.0.0.1:8080");
        sparkHistoryEndpointField.setText("http://127.0.0.1:18080");

        return super.createDialogArea(parent);
    }

    private boolean checkSshEndpoint() {
        try {
            session = jsch.getSession(userName, host, sshPort);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();
            session.disconnect();
        } catch (Exception e) {
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
            httpResponse = SparkBatchSubmission.getInstance()
                    .getHttpResponseViaGet(sparkHistoryEndpoint + "/api/v1/applications");
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

    private void postEmulatorSetupCheck() {
        if (isCarryOnNextStep && !checkLivyEndpoint()) {
            errorMessage = "Could not connect to livy, please check your endpoint";
            isCarryOnNextStep = false;
        } else if (isCarryOnNextStep && !checkSparkHistoryEndpoint()) {
            errorMessage = "Could not connect to spark history server, please check your endpoint.";
            isCarryOnNextStep = false;
        } else if (isCarryOnNextStep && !checkAmbariEndpoint()) {
            errorMessage = "Could not connect to ambari server, please check your endpoint.";
            isCarryOnNextStep = false;
        }
    }

    private void preEmulatorSetupCheck() {
        isCarryOnNextStep = true;
        errorMessage = null;
        emulatorSetupLog = new StringBuilder();
        errorMessageField.setText("");

        clusterName = clusterNameField.getText().trim();
        userName = userNameField.getText().trim();
        password = String.valueOf(passwordField.getText());
        livyEndpoint = livyEndpointField.getText().trim().replaceAll("/+$", "");
        sshEndpoint = sshEndpointField.getText().trim().replaceAll("/+$", "");
        sparkHistoryEndpoint = sparkHistoryEndpointField.getText().trim().replaceAll("/+$", "");
        ambariEndpoint = ambariEndpointField.getText().trim().replaceAll("/+$", "");

        AppInsightsClient.create(Messages.HDInsightCreateLocalEmulator, null);
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

        if (isCarryOnNextStep && StringHelper.isNullOrWhiteSpace(clusterName)
                || StringHelper.isNullOrWhiteSpace(sshEndpoint) || StringHelper.isNullOrWhiteSpace(livyEndpoint)
                || StringHelper.isNullOrWhiteSpace(userName) || StringHelper.isNullOrWhiteSpace(password)
                || StringHelper.isNullOrWhiteSpace(sparkHistoryEndpoint)
                || StringHelper.isNullOrWhiteSpace(ambariEndpoint)) {
            errorMessage = "Cluster Name, Endpoint fields, User Name, or Password shouldn't be empty";
            isCarryOnNextStep = false;
        } else if (isCarryOnNextStep && ClusterManagerEx.getInstance().isEmulatorClusterExist(clusterName)) {
            errorMessage = "Cluster Name already exist in current list";
            isCarryOnNextStep = false;
        } else if (isCarryOnNextStep && !checkSshEndpoint()) {
            errorMessage = "Could not ssh to emulator, please check username, password and ssh port.";
            isCarryOnNextStep = false;
        }
    }

    private void showEmulatorSetup() {
        if (isCarryOnNextStep) {
            EmulatorClusterDetail emulatorClusterDetail = new EmulatorClusterDetail(clusterName, userName, password,
                    livyEndpoint, sshEndpoint, sparkHistoryEndpoint, ambariEndpoint);
            ClusterManagerEx.getInstance().addEmulatorCluster(emulatorClusterDetail);
            hdInsightModule.load(false);
            super.okPressed();
        } else {
            errorMessageField.setText(errorMessage);
            errorMessageField.setVisible(true);
        }
    }

    private boolean checkEmulatorSetup() {
        boolean isEmulatorSetup = false;
        try {
            session = jsch.getSession(userName, host, sshPort);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();
            channel = (ChannelExec) session.openChannel("exec");
            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.setCommand(
                    String.format("curl --fail --silent -I %s | grep HTTP/1.1 | awk {'print $2'};", livyEndpoint));
            channel.connect();
            String result = in.readLine();
            if (result != null && result.equals("200")) {
                isEmulatorSetup = true;
            }

            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            return false;
        }

        return isEmulatorSetup;
    }

    @Override
    protected void okPressed() {
        synchronized (AddNewEmulatorForm.class) {
            preEmulatorSetupCheck();
            if(isCarryOnNextStep && !checkEmulatorSetup()) {
                emulatorSetupText.setVisible(true);
                setupLogLabel.setVisible(true);
                boolean answer =
                          MessageDialog.openQuestion(
                            getShell(),
                            "Question",
                            "We detect this emulator has not been configured yet. Would you like to set up emulator first (takes about 10 min)?");
                if(answer){
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    ListeningExecutorService executorService = MoreExecutors.listeningDecorator(executor);
                    ListenableFuture<Boolean> futureTask =  executorService.submit(new Callable<Boolean>()
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
                            URL url = StreamUtil.class.getResource(CommonConst.EmualtorPath + CommonConst.EmulatorArchieveFileName);
                            channelSftp.put(url.openStream(), CommonConst.EmulatorArchieveFileName);

                            URL url2 = StreamUtil.class.getResource(CommonConst.EmualtorPath + CommonConst.EmulatorSetupScriptFileName);
                            channelSftp.put(url2.openStream(), CommonConst.EmulatorSetupScriptFileName);

                            channel = (ChannelExec) session.openChannel("exec");
                            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                            channel.setCommand("perl -pi -e 's/\\r\\n/\\n/' setup.sh;chmod +x setup.sh;./setup.sh");
                            channel.connect();
                            String result = null;
                            while ((result = in.readLine()) != null) {
                                if (result.length() != 0) {
                                    emulatorSetupLog.append(result + "\n");
                                    Display.getDefault().asyncExec(new Runnable() {
                                        public void run() {
                                            emulatorSetupText.setText(emulatorSetupLog.toString());
                                        }
                                    });
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
                            postEmulatorSetupCheck();
                            showEmulatorSetup();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            errorMessage = throwable.getMessage();
                            isCarryOnNextStep = false;
                            showEmulatorSetup();
                        }
                    });
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
}
