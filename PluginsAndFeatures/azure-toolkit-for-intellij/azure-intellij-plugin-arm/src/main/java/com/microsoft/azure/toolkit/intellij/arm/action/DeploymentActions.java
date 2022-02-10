/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.arm.creation.CreateDeploymentDialog;
import com.microsoft.azure.toolkit.intellij.arm.template.ResourceTemplateViewProvider;
import com.microsoft.azure.toolkit.intellij.arm.update.UpdateDeploymentDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.FileChooser;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class DeploymentActions {
    private static final String TEMPLATE_SELECTOR_TITLE = "Choose Where to Save the ARM Template File.";
    private static final String PARAMETERS_SELECTOR_TITLE = "Choose Where to Save the ARM Parameter File.";

    private static final String TEMPLATE_FILE_NAME = "%s.template.json";
    private static final String PARAMETERS_FILE_NAME = "%s.parameters.json";

    public static final String NOTIFY_UPDATE_DEPLOYMENT_SUCCESS = "Update deployment successfully";
    public static final String NOTIFY_UPDATE_DEPLOYMENT_FAIL = "Update deployment failed";

    public static void createDeployment(@Nonnull final Project project, @Nullable ResourceGroup rg) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final CreateDeploymentDialog dialog = new CreateDeploymentDialog(project, rg);
            dialog.show();
        });
    }

    public static void openTemplateView(@Nonnull final Project project, @Nonnull ResourceDeployment deployment) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final Icon icon = AzureIcons.getIcon("/icons/Microsoft.Resources/resourceGroups/deployments/default.svg");
            final String name = ResourceTemplateViewProvider.TYPE;
            final AzureResourceEditorViewManager.AzureResourceFileType type = new AzureResourceEditorViewManager.AzureResourceFileType(name, icon);
            final AzureResourceEditorViewManager manager = new AzureResourceEditorViewManager((resource) -> type);
            manager.showEditor(deployment, project);
        });
    }

    public static void updateDeployment(@Nonnull final Project project, @Nonnull final ResourceDeployment deployment) {
        AzureTaskManager.getInstance().runLater(() -> new UpdateDeploymentDialog(project, deployment).show());
    }

    public static void exportTemplate(@Nonnull final ResourceDeployment deployment) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final File file = FileChooser.showFileSaver(TEMPLATE_SELECTOR_TITLE, String.format(TEMPLATE_FILE_NAME, deployment.getName()));
            if (file != null) {
                final String template = deployment.getTemplateAsJson();
                try {
                    IOUtils.write(template, new FileOutputStream(file), Charset.defaultCharset());
                } catch (final IOException e) {
                    throw new AzureToolkitRuntimeException(String.format("failed to write template to file \"%s\"", file.getName()), e);
                }
            }
        });
    }

    public static void exportParameters(final ResourceDeployment deployment) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final File file = FileChooser.showFileSaver(PARAMETERS_SELECTOR_TITLE, String.format(PARAMETERS_FILE_NAME, deployment.getName()));
            if (file != null) {
                final String parameters = deployment.getParametersAsJson();
                try {
                    IOUtils.write(parameters, new FileOutputStream(file), Charset.defaultCharset());
                } catch (final IOException e) {
                    throw new AzureToolkitRuntimeException(String.format("failed to write parameters to file \"%s\"", file.getName()), e);
                }
            }
        });
    }
}