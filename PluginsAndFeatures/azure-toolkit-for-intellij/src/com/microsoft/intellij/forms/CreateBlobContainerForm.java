/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.forms;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureText;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Calendar;
import java.util.List;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_BLOB_CONTAINER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateBlobContainerForm extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextField nameTextField;
    private JLabel namingGuidelinesLink;

    private Project project;
    private String connectionString;
    private Runnable onCreate;

    private static final String NAME_REGEX = "^[a-z0-9](?!.*--)[a-z0-9-]+[a-z0-9]$";
    private static final int NAME_MAX = 63;
    private static final int NAME_MIN = 3;

    public CreateBlobContainerForm(Project project) {
        super(project, true);
        this.project = project;

        setTitle("Create Blob Container");
        namingGuidelinesLink.addMouseListener(new LinkListener("http://go.microsoft.com/fwlink/?LinkId=255555"));

        init();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        String name = nameTextField.getText();
        if (name.isEmpty()) {
            return new ValidationInfo("Name cannot be empty", nameTextField);
        } else if (name.length() < NAME_MIN || name.length() > NAME_MAX || !name.matches(NAME_REGEX)) {
            return new ValidationInfo(
                    "Container names must start with a letter or number, and can contain only letters, numbers,"
                            + " and the dash (-) character.\nEvery dash (-) character must be immediately preceded "
                            + "and followed by a letter or number; consecutive dashes are not permitted in container "
                            + "names.\nAll letters in a container name must be lowercase.\nContainer names must be "
                            + "from 3 through 63 characters long.", nameTextField);
        }

        return null;
    }

    @Override
    protected void doOKAction() {
        final String name = nameTextField.getText();
        //Field outerFiele = onCreate.getClass().getDeclaredField("this$0");
        final AzureText title = AzureOperationBundle.title("blob.create", name);
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
            EventUtil.executeWithLog(STORAGE, CREATE_BLOB_CONTAINER, (operation) -> {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                List<BlobContainer> blobs = StorageClientSDKManager.getManager()
                                                                   .getBlobContainers(connectionString);
                for (BlobContainer blobContainer : blobs) {
                    if (blobContainer.getName().equals(name)) {
                        AzureTaskManager.getInstance().runLater(() -> {
                            DefaultLoader.getUIHelper().showError(
                                "A blob container with the specified name already exists.", "Azure Explorer");
                        });
                        return;
                    }
                }

                BlobContainer blobContainer = new BlobContainer(name,
                                                                ""/*storageAccount.getBlobsUri() + name*/, "", Calendar.getInstance(), "");
                StorageClientSDKManager.getManager().createBlobContainer(connectionString, blobContainer);

                if (onCreate != null) {
                    AzureTaskManager.getInstance().runLater(onCreate);
                }
            }, (e) -> {
                String msg = "An error occurred while attempting to create blob container."
                    + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
            });
        }));

        sendTelemetry(OK_EXIT_CODE);
        this.close(DialogWrapper.OK_EXIT_CODE, true);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }
}
