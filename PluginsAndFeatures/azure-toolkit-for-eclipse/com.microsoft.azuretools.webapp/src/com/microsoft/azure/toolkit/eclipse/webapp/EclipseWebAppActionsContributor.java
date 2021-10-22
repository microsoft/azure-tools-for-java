/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.webapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azureexplorer.editors.webapp.DeploymentSlotPropertyEditorInput;
import com.microsoft.azuretools.azureexplorer.editors.webapp.WebAppPropertyEditor;
import com.microsoft.azuretools.azureexplorer.editors.webapp.WebAppPropertyEditorInput;
import com.microsoft.azuretools.azureexplorer.helpers.EditorType;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class EclipseWebAppActionsContributor implements IActionsContributor {
    private static final String UNABLE_TO_OPEN_EXPLORER = "Unable to open explorer";
    
    @Override
    public void registerActions(AzureActionManager am) {
        final BiConsumer<AppServiceFile, Object> downloadHandler = (file, e) -> AzureTaskManager
                .getInstance().runLater(()->{
                    final File destFile = DefaultLoader.getUIHelper().showFileSaver(String.format("Download %s", file.getName()), file.getName());
                    if (destFile == null) {
                        return;
                    }
                    try {
                        OutputStream output = new FileOutputStream(destFile);
                        final AzureString title = AzureString.format("appservice|file.download", file.getName());
                        final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
                            file.getApp()
                                    .getFileContent(file.getPath())
                                    .doOnComplete(() -> AzureMessager.getMessager().info(AzureString.format("%s has been saved to %s", file.getName(), destFile.getAbsolutePath())))
                                    .doOnTerminate(() -> IOUtils.closeQuietly(output))
                                    .subscribe(bytes -> {
                                        try {
                                            if (bytes != null) {
                                                output.write(bytes.array(), 0, bytes.limit());
                                            }
                                        } catch (final IOException exception) {
                                            final String error = "failed to write data into local file";
                                            final String action = "try later";
                                            throw new AzureToolkitRuntimeException(error, exception, action);
                                        }
                                    });
                        });
                        AzureTaskManager.getInstance().runInModal(task);
                    } catch (FileNotFoundException e1) {
                        AzureMessager.getMessager().error(AzureString.format("Target file %s does not exists", destFile.getAbsolutePath()));
                    }
                });
        final ActionView.Builder downloadView = new ActionView.Builder("Download", "/icons/action/refresh")
                .title(s -> Optional.ofNullable(s)
                        .map(r -> AzureString.format("appservice|file.download", ((AppServiceFile) r).getName()))
                        .orElse(null))
                .enabled(s -> s instanceof AppServiceFile);
        am.registerAction(AppServiceFileActionsContributor.APP_SERVICE_FILE_DOWNLOAD, new Action<>(downloadHandler, downloadView));
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<IAzureBaseResource<?, ?>, Object> isWebApp = (r, e) -> r instanceof IWebApp;
        final BiConsumer<IAzureBaseResource<?, ?>, Object> openWebAppPropertyViewHandler = (c, e) -> AzureTaskManager
                .getInstance().runLater(() -> {
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    WebAppPropertyEditorInput input = new WebAppPropertyEditorInput(((IWebApp) c).subscriptionId(),
                            ((IWebApp) c).id(), ((IWebApp) c).name());
                    IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(WebAppPropertyEditor.ID);
                    openEditor(EditorType.WEBAPP_EXPLORER, input, descriptor);
                });
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, isWebApp, openWebAppPropertyViewHandler);

        final BiPredicate<IAzureBaseResource<?, ?>, Object> isWebAppSlot = (r, e) -> r instanceof IWebAppDeploymentSlot;
        final BiConsumer<IAzureBaseResource<?, ?>, Object> openWebAppSlotPropertyViewHandler = (c,
                e) -> AzureTaskManager.getInstance().runLater(() -> {
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    DeploymentSlotPropertyEditorInput input = new DeploymentSlotPropertyEditorInput(
                            ((IWebAppDeploymentSlot) c).id(), ((IWebAppDeploymentSlot) c).subscriptionId(),
                            ((IWebAppDeploymentSlot) c).webApp().id(), ((IWebAppDeploymentSlot) c).name());
                    IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(WebAppPropertyEditor.ID);
                    openEditor(EditorType.WEBAPP_EXPLORER, input, descriptor);
                });
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, isWebAppSlot,
                openWebAppSlotPropertyViewHandler);

        final BiConsumer<IAzureBaseResource<?, ?>, Object> deployWebAppHandler = (c, e) -> AzureTaskManager
                .getInstance().runLater(() -> {
                    try {
                        Command deployCommand = ((ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class))
                                .getCommand("com.microsoft.azuretools.webapp.commands.deployToAzure");
                        deployCommand.execute(new ExecutionEvent(null, Collections.singletonMap("resourceId", c.id()), c, null));
                    } catch (ExecutionException | NotHandledException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                });
        am.registerHandler(ResourceCommonActionsContributor.DEPLOY, isWebApp, deployWebAppHandler);
        
        final BiPredicate<Object, Object> createCondition = (r, e) -> r instanceof AzureWebApp;
        final BiConsumer<Object, Object> createHandler = (c, e) -> AzureTaskManager
                .getInstance().runLater(() -> {
                    try {
                        ((IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class)).executeCommand("com.microsoft.azuretools.webapp.commands.createWebApp", null);
                    } catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                });
        am.registerHandler(ResourceCommonActionsContributor.CREATE, createCondition, createHandler);
    }

    private void openEditor(EditorType type, IEditorInput input, IEditorDescriptor descriptor) {
        try {
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
            if (activeWorkbenchWindow == null) {
                return;
            }
            IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
            if (page == null) {
                return;
            }
            page.openEditor(input, descriptor.getId());
        } catch (Exception e) {
            AzureMessager.getMessager().error(UNABLE_TO_OPEN_EXPLORER);
        }
    }

    public int getOrder() {
        return 2;
    }
}
