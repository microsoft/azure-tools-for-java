/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.functionapp;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import com.microsoft.azure.toolkit.lib.Azure;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;

import com.microsoft.azure.toolkit.eclipse.appservice.property.AppServicePropertyEditorInput;
import com.microsoft.azure.toolkit.eclipse.functionapp.creation.CreateFunctionAppHandler;
import com.microsoft.azure.toolkit.eclipse.functionapp.logstreaming.FunctionAppLogStreamingHandler;
import com.microsoft.azure.toolkit.eclipse.functionapp.property.FunctionAppPropertyEditor;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.model.TriggerRequest;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.AzureFunction;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class EclipseFunctionAppActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = FunctionAppActionsContributor.INITIALIZE_ORDER + 1;

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, Object> createCondition = (r, e) -> r instanceof AzureFunction;
        final BiConsumer<Object, Object> createHandler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> CreateFunctionAppHandler.create());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, createCondition, createHandler);

        final BiPredicate<IAzureBaseResource<?, ?>, Object> isFunctionApp = (r, e) -> r instanceof FunctionApp;
        final BiConsumer<IAzureBaseResource<?, ?>, Object> openWebAppPropertyViewHandler = (c, e) -> AzureTaskManager
                .getInstance().runLater(() -> {
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    AppServicePropertyEditorInput input = new AppServicePropertyEditorInput(c.id());
                    IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(FunctionAppPropertyEditor.ID);
                    openEditor(input, descriptor);
                });
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, isFunctionApp, openWebAppPropertyViewHandler);
        
        final BiPredicate<IAppService<?>, Object> logStreamingPredicate = (r, e) -> r instanceof IFunctionAppBase<?>;
        final BiConsumer<IAppService<?>, Object> startLogStreamingHandler = (c, e) -> FunctionAppLogStreamingHandler
                .startLogStreaming((IFunctionAppBase<?>) c);
        am.registerHandler(AppServiceActionsContributor.START_STREAM_LOG, logStreamingPredicate,
                startLogStreamingHandler);

        final BiConsumer<IAppService<?>, Object> stopLogStreamingHandler = (c, e) -> FunctionAppLogStreamingHandler
                .stopLogStreaming((IFunctionAppBase<?>) c);
        am.registerHandler(AppServiceActionsContributor.STOP_STREAM_LOG, logStreamingPredicate,
                stopLogStreamingHandler);

        // todo: Remove duplicated codes with IntelliJ
        final BiPredicate<FunctionEntity, Object> triggerPredicate = (r, e) -> r instanceof FunctionEntity;
        final BiConsumer<FunctionEntity, Object> triggerFunctionHandler = (entity, e) -> {
            final String functionId = Optional.ofNullable(entity.getFunctionAppId())
                    .orElseGet(() -> ResourceId.fromString(entity.getTriggerId()).parent().id());
            final FunctionApp functionApp = Azure.az(AzureFunction.class).get(functionId);
            final String triggerType = Optional.ofNullable(entity.getTrigger())
                    .map(functionTrigger -> functionTrigger.getProperty("type")).orElse(null);
            final Object request;
            if (StringUtils.equalsIgnoreCase(triggerType, "timertrigger")) {
                request = new Object();
            } else {
                final String input = DefaultLoader.getUIHelper().showInputDialog(null, "Please set the input value: ",
                        String.format("Trigger function %s", entity.getName()), null);
                request = new TriggerRequest(input);
            }
            functionApp.triggerFunction(entity.getName(), request);

        };
        am.registerHandler(FunctionAppActionsContributor.TRIGGER_FUNCTION, triggerPredicate, triggerFunctionHandler);
    }

    // todo: remove duplicated with EclipseWebAppActionsContributor
    private void openEditor(IEditorInput input, IEditorDescriptor descriptor) {
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
            AzureMessager.getMessager().error(e, "Unable to open editor view");
        }
    }
    
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
