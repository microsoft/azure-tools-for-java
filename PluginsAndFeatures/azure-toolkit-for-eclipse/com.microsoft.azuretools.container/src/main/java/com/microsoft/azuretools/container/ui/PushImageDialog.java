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

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACR;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACR_PUSHIMAGE;

import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.azurecommons.exceptions.InvalidFormDataException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerProgressHandler;
import com.microsoft.azuretools.container.ui.common.ContainerSettingComposite;
import com.microsoft.azuretools.container.utils.DockerUtil;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.mvp.model.container.pojo.PushImageRunModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

import rx.Observable;

public class PushImageDialog extends AzureTitleAreaDialogWrapper {

    private final PushImageRunModel model;
    private final String basePath;
    private final String targetPath;

    // TODO: move to util
    private static final String MISSING_ARTIFACT = "A web archive (.war) artifact has not been configured.";
    private static final String MISSING_SERVER_URL = "Please specify a valid Server URL.";
    private static final String MISSING_USERNAME = "Please specify Username.";
    private static final String MISSING_PASSWORD = "Please specify Password.";
    private static final String MISSING_IMAGE_WITH_TAG = "Please specify Image and Tag.";
    private static final String INVALID_DOCKER_FILE = "Please specify a valid docker file.";
    private static final String INVALID_IMAGE_WITH_TAG = "Image and Tag name is invalid";
    private static final String INVALID_ARTIFACT_FILE = "The artifact name %s is invalid. "
            + "An artifact name may contain only the ASCII letters 'a' through 'z' (case-insensitive), "
            + "and the digits '0' through '9', '.', '-' and '_'.";
    private static final String CANNOT_END_WITH_COLON = "Image and tag name cannot end with ':'";
    private static final String REPO_LENGTH_INVALID = "The length of repository name must be at least one character "
            + "and less than 256 characters";
    private static final String CANNOT_END_WITH_SLASH = "The repository name should not end with '/'";
    private static final String REPO_COMPONENT_INVALID = "Invalid repository component: %s, should follow: %s";
    private static final String TAG_LENGTH_INVALID = "The length of tag name must be no more than 128 characters";
    private static final String TAG_INVALID = "Invalid tag: %s, should follow: %s";
    private static final String MISSING_MODEL = "Configuration data model not initialized.";
    private static final String ARTIFACT_NAME_REGEX = "^[.A-Za-z0-9_-]+\\.(war|jar)$";
    private static final String DOMAIN_NAME_REGEX = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";
    private static final String REPO_COMPONENTS_REGEX = "[a-z0-9]+(?:[._-][a-z0-9]+)*";
    private static final String TAG_REGEX = "^[\\w]+[\\w.-]*$";
    private static final int TAG_LENGTH = 128;
    private static final int REPO_LENGTH = 255;

    private ContainerSettingComposite containerSettingComposite;

    /**
     * Create the dialog.
     */
    public PushImageDialog(Shell parentShell, String basePath, String targetPath) {
        super(parentShell);
        setShellStyle(SWT.RESIZE | SWT.TITLE);
        this.basePath = basePath;
        this.targetPath = targetPath;
        model = new PushImageRunModel();
    }

    /**
     * Create contents of the dialog.
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        containerSettingComposite = new ContainerSettingComposite(area, SWT.NONE, basePath);
        containerSettingComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        containerSettingComposite.setStartupFileVisible(false);
        containerSettingComposite.addTxtServerUrlModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                parent.getShell().layout(true, true);
                parent.getShell().pack(true);
            }
        });

        setTitle("Push Image");
        setMessage("Configure the container registry you want to push to.");

        reset();
        return area;
    }

    private void reset() {
        // set default Dockerfile path
        String defaultDockerFilePath = DockerUtil.getDefaultDockerFilePathIfExist(basePath);
        containerSettingComposite.setDockerfilePath(defaultDockerFilePath);

        // list registries
        containerSettingComposite.onListRegistries();
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
        model.setDockerFilePath(containerSettingComposite.getDockerfilePath());
        // set ACR info
        model.setPrivateRegistryImageSetting(new PrivateRegistryImageSetting(
                containerSettingComposite.getServerUrl().replaceFirst("^https?://", "").replaceFirst("/$", ""),
                containerSettingComposite.getUserName(), containerSettingComposite.getPassword(),
                containerSettingComposite.getImageTag(), ""));

        // set target
        model.setTargetPath(targetPath);
        model.setTargetName(Paths.get(targetPath).getFileName().toString());
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
        // acr
        PrivateRegistryImageSetting setting = model.getPrivateRegistryImageSetting();
        if (Utils.isEmptyString(setting.getServerUrl()) || !setting.getServerUrl().matches(DOMAIN_NAME_REGEX)) {
            throw new InvalidFormDataException(MISSING_SERVER_URL);
        }
        if (Utils.isEmptyString(setting.getUsername())) {
            throw new InvalidFormDataException(MISSING_USERNAME);
        }
        if (Utils.isEmptyString(setting.getPassword())) {
            throw new InvalidFormDataException(MISSING_PASSWORD);
        }
        String imageTag = setting.getImageTagWithServerUrl();
        if (Utils.isEmptyString(imageTag)) {
            throw new InvalidFormDataException(MISSING_IMAGE_WITH_TAG);
        }
        if (imageTag.endsWith(":")) {
            throw new InvalidFormDataException(CANNOT_END_WITH_COLON);
        }
        final String[] repoAndTag = imageTag.split(":");

        // check repository first
        if (repoAndTag[0].length() < 1 || repoAndTag[0].length() > REPO_LENGTH) {
            throw new InvalidFormDataException(REPO_LENGTH_INVALID);
        }
        if (repoAndTag[0].endsWith("/")) {
            throw new InvalidFormDataException(CANNOT_END_WITH_SLASH);
        }
        final String[] repoComponents = repoAndTag[0].split("/");
        for (String component : repoComponents) {
            if (!component.matches(REPO_COMPONENTS_REGEX)) {
                throw new InvalidFormDataException(
                        String.format(REPO_COMPONENT_INVALID, component, REPO_COMPONENTS_REGEX));
            }
        }
        // check when contains tag
        if (repoAndTag.length == 2) {
            if (repoAndTag[1].length() > TAG_LENGTH) {
                throw new InvalidFormDataException(TAG_LENGTH_INVALID);
            }
            if (!repoAndTag[1].matches(TAG_REGEX)) {
                throw new InvalidFormDataException(String.format(TAG_INVALID, repoAndTag[1], TAG_REGEX));
            }
        }
        if (repoAndTag.length > 2) {
            throw new InvalidFormDataException(INVALID_IMAGE_WITH_TAG);
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
        Operation operation = TelemetryManager.createOperation(ACR, ACR_PUSHIMAGE);
        Observable.fromCallable(() -> {
            ConsoleLogger.info("Starting job ...  ");
            operation.start();
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
            PrivateRegistryImageSetting acrInfo = model.getPrivateRegistryImageSetting();
            ConsoleLogger.info(String.format("Building image ...  [%s]", acrInfo.getImageTagWithServerUrl()));
            DockerClient docker = DefaultDockerClient.fromEnv().build();
            DockerUtil.buildImage(docker, acrInfo.getImageTagWithServerUrl(), targetDockerfile.getParent(),
                    targetDockerfile.getFileName().toString(), new DockerProgressHandler());

            // push to ACR
            ConsoleLogger.info(String.format("Pushing to ACR ... [%s] ", acrInfo.getServerUrl()));
            DockerUtil.pushImage(docker, acrInfo.getServerUrl(), acrInfo.getUsername(), acrInfo.getPassword(),
                    acrInfo.getImageTagWithServerUrl(), new DockerProgressHandler());

            return null;
        }).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
            props -> {
                ConsoleLogger.info("pushed.");
                sendTelemetry(true, null);
                operation.complete();
            },
            err -> {
                err.printStackTrace();
                ConsoleLogger.error(err.getMessage());
                EventUtil.logError(operation, ErrorType.systemError, new Exception(err), null, null);
                operation.complete();
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
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "Container Registry", "PushImage", map);
    }

    private void showErrorMessage(String title, String message) {
        MessageDialog.openError(this.getShell(), title, message);
    }
}
