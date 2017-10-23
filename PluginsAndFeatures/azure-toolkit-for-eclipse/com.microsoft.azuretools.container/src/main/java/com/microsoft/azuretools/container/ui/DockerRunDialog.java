/**
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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import com.microsoft.azuretools.azurecommons.exceptions.InvalidFormDataException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.ui.common.FileSelector;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

import rx.Observable;

public class DockerRunDialog extends AzureTitleAreaDialogWrapper {

    private final String basePath;
    private final String targetPath;

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

    private DockerHostRunSetting model;
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
        this.basePath = basePath;
        this.targetPath = targetPath;
    }

    /**
     * Create contents of the dialog.
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        area.setLayout(new GridLayout(1, false));

        Composite composite = new Composite(area, SWT.NONE);
        composite.setLayout(new GridLayout(5, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        Label lblDockerFile = new Label(composite, SWT.NONE);
        lblDockerFile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblDockerFile.setText("Docker File");

        dockerFileSelector = new FileSelector(composite, SWT.NONE, false, "...", null);
        dockerFileSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

        Label lblDockerHost = new Label(composite, SWT.NONE);
        lblDockerHost.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblDockerHost.setText("Docker Host");

        txtDockerHost = new Text(composite, SWT.BORDER);
        txtDockerHost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

        btnTlsEnabled = new Button(composite, SWT.CHECK);
        btnTlsEnabled.setText("Enable TLS");

        Label lblCertPath = new Label(composite, SWT.NONE);
        lblCertPath.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblCertPath.setText("Cert Path");

        certPathSelector = new FileSelector(composite, SWT.NONE, true, "...", null);
        certPathSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        Label lblImage = new Label(composite, SWT.NONE);
        lblImage.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblImage.setText("Image Name");

        txtImageName = new Text(composite, SWT.BORDER);
        txtImageName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Label lblTagName = new Label(composite, SWT.NONE);
        lblTagName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblTagName.setText("Tag Name");

        txtTagName = new Text(composite, SWT.BORDER);
        txtTagName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        setTitle("TBD");
        setMessage("TBD");

        return area;
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
        model.setTlsEnabled(btnTlsEnabled.getSelection());
        model.setDockerFilePath(dockerFileSelector.getFilePath());
        model.setDockerCertPath(certPathSelector.getFilePath());
        model.setDockerHost(txtDockerHost.getText());
        model.setImageName(txtImageName.getText());
        model.setTagName(txtTagName.getText());
    }


    private void validate() throws InvalidFormDataException {
        if (model == null) {
            throw new InvalidFormDataException(MISSING_MODEL);
        }
        // docker file
        if (Utils.isEmptyString(model.getDockerFilePath())) {
            throw new InvalidFormDataException(INVALID_DOCKER_FILE);
        }
        File dockerFile = Paths.get(model.getDockerFilePath()).toFile();
        if (!dockerFile.exists() || !dockerFile.isFile()) {
            throw new InvalidFormDataException(INVALID_DOCKER_FILE);
        }
        // cert path
        if (model.isTlsEnabled()) {
            if (Utils.isEmptyString(model.getDockerCertPath())) {
                throw new InvalidFormDataException(INVALID_CERT_PATH);
            }
            File certPath = Paths.get(model.getDockerFilePath()).toFile();
            if (!certPath.exists() || !certPath.isDirectory()) {
                throw new InvalidFormDataException(INVALID_CERT_PATH);
            }
        }

        String imageName = model.getImageName();
        String tagName = model.getTagName();
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
        if (Utils.isEmptyString(model.getTargetName())) {
            throw new InvalidFormDataException(MISSING_ARTIFACT);
        }
        if (!model.getTargetName().matches(ARTIFACT_NAME_REGEX)) {
            throw new InvalidFormDataException(String.format(INVALID_ARTIFACT_FILE, model.getTargetName()));
        }
    }

    private void execute() {
        Observable.fromCallable(() -> {
            ConsoleLogger.info("Starting job ...  ");
            if (basePath == null) {
                ConsoleLogger.error("Project base path is null.");
                throw new FileNotFoundException("Project base path is null.");
            }
            // locate artifact to specified location
            String targetFilePath = model.getTargetPath();
            ConsoleLogger.info(String.format("Locating artifact ... [%s]", targetFilePath));

            // validate dockerfile
            Path targetDockerfile = Paths.get(model.getDockerFilePath());
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
            ConsoleLogger.info(String.format("Building image ...  [%s]", "image"));
            DockerClient docker = DefaultDockerClient.fromEnv().build();
            // TODO: build image
            //          DockerUtil.buildImage(docker, acrInfo.getImageTagWithServerUrl(), targetDockerfile.getParent(),
            //                    targetDockerfile.getFileName().toString(), new DockerProgressHandler());

            // TODO: create container and start

            return null;
        }).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
            props -> {
                ConsoleLogger.info("started.");
                sendTelemetry(true, null);
            },
            err -> {
                err.printStackTrace();
                ConsoleLogger.error(err.getMessage());
                sendTelemetry(false, err.getMessage());
            }
        );
    }

    // TODO: refactor later
    private void sendTelemetry(boolean success, @Nullable String errorMsg) {
        Map<String, String> map = new HashMap<>();
        map.put("Success", String.valueOf(success));
        if (null != model.getTargetName()) {
            map.put("FileType", FilenameUtils.getExtension(model.getTargetName()));
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
