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

package com.microsoft.intellij.forms;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Calendar;

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
            return new ValidationInfo("Container names must start with a letter or number, and can contain only letters, numbers, and the dash (-) character.\n" +
                    "Every dash (-) character must be immediately preceded and followed by a letter or number; consecutive dashes are not permitted in container names.\n" +
                    "All letters in a container name must be lowercase.\n" +
                    "Container names must be from 3 through 63 characters long.", nameTextField);
        }

        return null;
    }

    @Override
    protected void doOKAction() {
        final String name = nameTextField.getText();
        //Field outerFiele = onCreate.getClass().getDeclaredField("this$0");
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating blob container...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                EventUtil.executeWithLog(STORAGE, CREATE_BLOB_CONTAINER, (operation) -> {
                    progressIndicator.setIndeterminate(true);
                    List<BlobContainer> blobs = StorageClientSDKManager.getManager()
                        .getBlobContainers(connectionString);
                    for (BlobContainer blobContainer : blobs) {
                        if (blobContainer.getName().equals(name)) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                JOptionPane.showMessageDialog(null,
                                    "A blob container with the specified name already exists.",
                                    "Azure Explorer", JOptionPane.ERROR_MESSAGE);
                            });
                            return;
                        }
                    }

                    BlobContainer blobContainer = new BlobContainer(name,
                        ""/*storageAccount.getBlobsUri() + name*/, "", Calendar.getInstance(), "");
                    StorageClientSDKManager.getManager().createBlobContainer(connectionString, blobContainer);

                    if (onCreate != null) {
                        ApplicationManager.getApplication().invokeLater(onCreate);
                    }
                }, (e) -> {
                    String msg = "An error occurred while attempting to create blob container."
                        + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
                });
            }
        });

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
