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

package com.microsoft.azuretools.container.ui;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DEPLOY_WEBAPP_DOCKERLOCAL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;
import com.microsoft.azuretools.azurecommons.exceptions.InvalidFormDataException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerProgressHandler;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.ui.common.FileSelector;
import com.microsoft.azuretools.container.utils.DockerUtil;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.Container.PortMapping;

import rx.Observable;

public class DockerRunDialog extends AzureTitleAreaDialogWrapper {

    private final String basePath;

    // TODO: move to util
    private static final String MISSING_ARTIFACT = "A web archive (.war) artifact has not been configured.";
    private static final String MISSING_IMAGE_WITH_TAG = "Please specify Image and Tag.";
    private static final String INVALID_DOCKER_FILE = "Please specify a valid docker file.";
    private static final String INVALID_CERT_PATH = "Please specify a valid certificate path.";
    private static final String INVALID_ARTIFACT_FILE = "The artifact name %s is invalid. "
            + "An artifact name may contain only the ASCII letters 'a' through 'z' (case-insensitive), "
            + "and the digits '0' through '9', '.', '-' and '_'.";
    private static final String REPO_LENGTH_INVALID = "The length of repository name must be at least one character "
            + "and less than 256 characters";
    private static final String CANNOT_END_WITH_SLASH = "The repository name should not end with '/'";
    private static final String REPO_COMPONENT_INVALID = "Invalid repository component: %s, should follow: %s";
    private static final String TAG_LENGTH_INVALID = "The length of tag name must be no more than 128 characters";
    private static final String TAG_INVALID = "Invalid tag: %s, should follow: %s";
    private static final String MISSING_MODEL = "Configuration data model not initialized.";
    private static final String ARTIFACT_NAME_REGEX = "^[.A-Za-z0-9_-]+\\.(war|jar)$";
    private static final String REPO_COMPONENTS_REGEX = "[a-z0-9]+(?:[._-][a-z0-9]+)*";
    private static final String TAG_REGEX = "^[\\w]+[\\w.-]*$";
    private static final int TAG_LENGTH = 128;
    private static final int REPO_LENGTH = 255;
    private static final String IMAGE_NAME_PREFIX = "localimage";
    private static final String DEFAULT_TAG_NAME = "latest";
    private static final String SELECT_DOCKER_FILE = "Browse...";

    private DockerHostRunSetting dataModel;
    private Text txtDockerHost;
    private Text txtImageName;
    private Text txtTagName;
    private Button btnTlsEnabled;
    private FileSelector dockerFileSelector;
    private FileSelector certPathSelector;

    /**
     * Create the dialog.
     */
    public DockerRunDialog(Shell parentShell, String basePath, String targetPath) {
        super(parentShell);
        setShellStyle(SWT.RESIZE | SWT.TITLE);
        this.basePath = basePath;
        dataModel = new DockerHostRunSetting();
        dataModel.setTargetPath(targetPath);
        dataModel.setTargetName(FilenameUtils.getName(targetPath));
    }

    /**
     * Create contents of the dialog.
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite composite = new Composite(area, SWT.NONE);
        composite.setLayout(new GridLayout(5, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        dockerFileSelector = new FileSelector(composite, SWT.NONE, false, SELECT_DOCKER_FILE, basePath, "Docker File");
        dockerFileSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));

        Label lblDockerHost = new Label(composite, SWT.NONE);
        lblDockerHost.setText("Docker Host");

        txtDockerHost = new Text(composite, SWT.BORDER);
        txtDockerHost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

        btnTlsEnabled = new Button(composite, SWT.CHECK);
        btnTlsEnabled.addListener(SWT.Selection, event -> onBtnTlsEnabledSelection());
        btnTlsEnabled.setText("Enable TLS");

        certPathSelector = new FileSelector(composite, SWT.NONE, true, "Browse...", null, "Cert Path");
        certPathSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

        Label lblImage = new Label(composite, SWT.NONE);
        lblImage.setText("Image Name");

        txtImageName = new Text(composite, SWT.BORDER);
        txtImageName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Label lblTagName = new Label(composite, SWT.NONE);
        lblTagName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblTagName.setText("Tag Name");

        txtTagName = new Text(composite, SWT.BORDER);
        txtTagName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        setTitle("Run on Docker Host");
        setMessage(""); // TOOD: specify the message.

        reset();
        return area;
    }

    private void reset() {
        // set default dockerHost value
        if (Utils.isEmptyString(txtDockerHost.getText())) {
            try {
                txtDockerHost.setText(DefaultDockerClient.fromEnv().uri().toString());
            } catch (DockerCertificateException e) {
                e.printStackTrace();
            }
        }
        // set default Dockerfile path
        String defaultDockerFilePath = DockerUtil.getDefaultDockerFilePathIfExist(basePath);
        dockerFileSelector.setFilePath(defaultDockerFilePath);

        // set default image and tag
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());
        if (Utils.isEmptyString(txtImageName.getText())) {
            txtImageName.setText(String.format("%s-%s", IMAGE_NAME_PREFIX, date));
        }
        if (Utils.isEmptyString(txtTagName.getText())) {
            txtTagName.setText(DEFAULT_TAG_NAME);
        }

        updateCertPathVisibility();
    }

    private void onBtnTlsEnabledSelection() {
        updateCertPathVisibility();
    }

    private void updateCertPathVisibility() {
        certPathSelector.setVisible(btnTlsEnabled.getSelection());
    }

    /**
     * Create contents of the button bar.
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        this.getShell().layout(true, true);
        return this.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public boolean isHelpAvailable() {
        return false;
    }

    @Override
    protected void okPressed() {
        apply();
        try {
            validate();
            execute();
            super.okPressed();
        } catch (InvalidFormDataException e) {
            showErrorMessage("Error", e.getMessage());
        }
    }

    private void apply() {
        dataModel.setTlsEnabled(btnTlsEnabled.getSelection());
        dataModel.setDockerFilePath(dockerFileSelector.getFilePath());
        dataModel.setDockerCertPath(certPathSelector.getFilePath());
        dataModel.setDockerHost(txtDockerHost.getText());
        dataModel.setImageName(txtImageName.getText());
        dataModel.setTagName(txtTagName.getText());
    }

    private void validate() throws InvalidFormDataException {
        if (dataModel == null) {
            throw new InvalidFormDataException(MISSING_MODEL);
        }
        // docker file
        if (Utils.isEmptyString(dataModel.getDockerFilePath())) {
            throw new InvalidFormDataException(INVALID_DOCKER_FILE);
        }
        File dockerFile = Paths.get(dataModel.getDockerFilePath()).toFile();
        if (!dockerFile.exists() || !dockerFile.isFile()) {
            throw new InvalidFormDataException(INVALID_DOCKER_FILE);
        }
        // cert path
        if (dataModel.isTlsEnabled()) {
            if (Utils.isEmptyString(dataModel.getDockerCertPath())) {
                throw new InvalidFormDataException(INVALID_CERT_PATH);
            }
            File certPath = Paths.get(dataModel.getDockerCertPath()).toFile();
            if (!certPath.exists() || !certPath.isDirectory()) {
                throw new InvalidFormDataException(INVALID_CERT_PATH);
            }
        }

        String imageName = dataModel.getImageName();
        String tagName = dataModel.getTagName();
        if (Utils.isEmptyString(imageName) || Utils.isEmptyString(tagName)) {
            throw new InvalidFormDataException(MISSING_IMAGE_WITH_TAG);
        }

        // check repository first
        if (imageName.length() < 1 || imageName.length() > REPO_LENGTH) {
            throw new InvalidFormDataException(REPO_LENGTH_INVALID);
        }
        if (imageName.endsWith("/")) {
            throw new InvalidFormDataException(CANNOT_END_WITH_SLASH);
        }
        final String[] repoComponents = imageName.split("/");
        for (String component : repoComponents) {
            if (!component.matches(REPO_COMPONENTS_REGEX)) {
                throw new InvalidFormDataException(
                        String.format(REPO_COMPONENT_INVALID, component, REPO_COMPONENTS_REGEX));
            }
        }
        // check tag
        if (tagName.length() > TAG_LENGTH) {
            throw new InvalidFormDataException(TAG_LENGTH_INVALID);
        }
        if (!tagName.matches(TAG_REGEX)) {
            throw new InvalidFormDataException(String.format(TAG_INVALID, tagName, TAG_REGEX));
        }

        // target package
        if (Utils.isEmptyString(dataModel.getTargetName())) {
            throw new InvalidFormDataException(MISSING_ARTIFACT);
        }
        if (!dataModel.getTargetName().matches(ARTIFACT_NAME_REGEX)) {
            throw new InvalidFormDataException(String.format(INVALID_ARTIFACT_FILE, dataModel.getTargetName()));
        }
    }

    private void execute() {
        Operation operation = TelemetryManager.createOperation(WEBAPP, DEPLOY_WEBAPP_DOCKERLOCAL);
        Observable.fromCallable(() -> {
            operation.start();
            ConsoleLogger.info("Starting job ...  ");
            if (basePath == null) {
                ConsoleLogger.error("Project base path is null.");
                throw new FileNotFoundException("Project base path is null.");
            }
            // locate artifact to specified location
            String targetFilePath = dataModel.getTargetPath();
            ConsoleLogger.info(String.format("Locating artifact ... [%s]", targetFilePath));

            // validate dockerfile
            Path targetDockerfile = Paths.get(dataModel.getDockerFilePath());
            ConsoleLogger.info(String.format("Validating dockerfile ... [%s]", targetDockerfile));
            if (!targetDockerfile.toFile().exists()) {
                throw new FileNotFoundException("Dockerfile not found.");
            }

            // replace placeholder if exists
            String content = new String(Files.readAllBytes(targetDockerfile));
            content = content.replaceAll(Constant.DOCKERFILE_ARTIFACT_PLACEHOLDER,
                    Paths.get(basePath).toUri().relativize(Paths.get(targetFilePath).toUri()).getPath());
            Files.write(targetDockerfile, content.getBytes());

            // build image
            String imageNameWithTag = String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName());
            ConsoleLogger.info(String.format("Building image ...  [%s]", imageNameWithTag));
            DockerClient docker = DockerUtil.getDockerClient(dataModel.getDockerHost(), dataModel.isTlsEnabled(),
                    dataModel.getDockerCertPath());
            DockerUtil.buildImage(docker, imageNameWithTag, targetDockerfile.getParent(),
                    targetDockerfile.getFileName().toString(), new DockerProgressHandler());

            // create a container
            ConsoleLogger.info(Constant.MESSAGE_CREATING_CONTAINER);
            String containerId = DockerUtil.createContainer(docker,
                    String.format("%s:%s", dataModel.getImageName(), dataModel.getTagName()));
            ConsoleLogger.info(String.format(Constant.MESSAGE_CONTAINER_INFO, containerId));

            // start container
            ConsoleLogger.info(Constant.MESSAGE_STARTING_CONTAINER);
            Container container = DockerUtil.runContainer(docker, containerId);
            DockerRuntime.getInstance().setRunningContainerId(basePath, container.id(), dataModel);

            // props
            String hostname = new URI(dataModel.getDockerHost()).getHost();
            ImmutableList<PortMapping> ports = container.ports();
            String publicPort = null;
            if (ports != null) {
                for (Container.PortMapping portMapping : ports) {
                    if (Constant.TOMCAT_SERVICE_PORT.equals(String.valueOf(portMapping.privatePort()))) {
                        publicPort = String.valueOf(portMapping.publicPort());
                    }
                }
            }

            ConsoleLogger.info(String.format(Constant.MESSAGE_CONTAINER_STARTED,
                    (hostname != null ? hostname : "localhost") + (publicPort != null ? ":" + publicPort : "")));
            return null;
        }).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
            ret -> {
                ConsoleLogger.info("Container started.");
                sendTelemetry(true, null);
                operation.complete();
            },
            e -> {
                e.printStackTrace();
                ConsoleLogger.error(e.getMessage());
                sendTelemetry(false, e.getMessage());
                EventUtil.logError(operation, ErrorType.systemError, new Exception(e), null, null);
                operation.complete();
            }
        );
    }

    // TODO: refactor later
    private void sendTelemetry(boolean success, @Nullable String errorMsg) {
        Map<String, String> map = new HashMap<>();
        map.put("Success", String.valueOf(success));
        if (null != dataModel.getTargetName()) {
            map.put("FileType", FilenameUtils.getExtension(dataModel.getTargetName()));
        } else {
            map.put("FileType", "");
        }
        if (!success) {
            map.put("ErrorMsg", errorMsg);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "Docker", "Run", map);
    }

    private void showErrorMessage(String title, String message) {
        MessageDialog.openError(this.getShell(), title, message);
    }
}
