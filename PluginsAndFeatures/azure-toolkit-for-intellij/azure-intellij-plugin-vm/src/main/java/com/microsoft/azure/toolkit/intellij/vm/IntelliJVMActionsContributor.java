/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.vm.creation.CreateVirtualMachineAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.VirtualMachine;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntelliJVMActionsContributor implements IActionsContributor {
    // todo: Implement action contributor for vm and migrate explorer tree from utils
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> createCondition = (r, e) -> r instanceof AzureVirtualMachine;
        final BiConsumer<Object, AnActionEvent> createHandler = (c, e) -> CreateVirtualMachineAction.createVirtualMachine((e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, createCondition, createHandler);

        final BiPredicate<IAzureBaseResource<?, ?>, AnActionEvent> startCondition = (r, e) -> r instanceof VirtualMachine &&
                StringUtils.equals(((VirtualMachine) r).status(), IAzureBaseResource.Status.STOPPED);
        final BiConsumer<IAzureBaseResource<?, ?>, AnActionEvent> startHandler = (c, e) -> ((VirtualMachine) c).start();
        am.registerHandler(ResourceCommonActionsContributor.START, startCondition, startHandler);

        final BiPredicate<IAzureBaseResource<?, ?>, AnActionEvent> stopCondition = (r, e) -> r instanceof VirtualMachine &&
                StringUtils.equals(((VirtualMachine) r).status(), IAzureBaseResource.Status.RUNNING);
        final BiConsumer<IAzureBaseResource<?, ?>, AnActionEvent> stopHandler = (c, e) -> ((VirtualMachine) c).stop();
        am.registerHandler(ResourceCommonActionsContributor.STOP, stopCondition, stopHandler);

        final BiPredicate<IAzureBaseResource<?, ?>, AnActionEvent> restartCondition = (r, e) -> r instanceof VirtualMachine &&
                StringUtils.equals(((VirtualMachine) r).status(), IAzureBaseResource.Status.RUNNING);
        final BiConsumer<IAzureBaseResource<?, ?>, AnActionEvent> restartHandler = (c, e) -> ((VirtualMachine) c).restart();
        am.registerHandler(ResourceCommonActionsContributor.RESTART, restartCondition, restartHandler);
    }

    @Override
    public int getOrder() {
        return IActionsContributor.super.getOrder() + 1;
    }
}
