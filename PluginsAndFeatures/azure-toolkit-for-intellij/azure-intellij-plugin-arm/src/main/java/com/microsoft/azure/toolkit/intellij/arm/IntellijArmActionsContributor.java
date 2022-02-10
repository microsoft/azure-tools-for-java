/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.arm.ArmActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.arm.action.DeploymentActions;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijArmActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> createCondition1 = (r, e) -> r instanceof AzureResources;
        final BiConsumer<Object, AnActionEvent> createHandler1 = (c, e) -> DeploymentActions.createDeployment((e.getProject()), null);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, createCondition1, createHandler1);

        final BiPredicate<Object, AnActionEvent> createCondition2 = (r, e) -> r instanceof ResourceGroup;
        final BiConsumer<Object, AnActionEvent> createHandler2 = (c, e) -> DeploymentActions.createDeployment((e.getProject()), (ResourceGroup) c);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, createCondition2, createHandler2);

        final BiPredicate<ResourceDeployment, AnActionEvent> editCondition = (r, e) -> r instanceof ResourceDeployment;
        final BiConsumer<ResourceDeployment, AnActionEvent> editHandler = (c, e) -> DeploymentActions.openTemplateView(e.getProject(), c);
        am.registerHandler(ArmActionsContributor.EDIT, editCondition, editHandler);

        final BiPredicate<ResourceDeployment, AnActionEvent> updateCondition = (r, e) -> r instanceof ResourceDeployment;
        final BiConsumer<ResourceDeployment, AnActionEvent> updateHandler = (c, e) -> DeploymentActions.updateDeployment(e.getProject(), c);
        am.registerHandler(ArmActionsContributor.UPDATE, updateCondition, updateHandler);

        final BiPredicate<ResourceDeployment, AnActionEvent> exportParameterCondition = (r, e) -> r instanceof ResourceDeployment;
        final BiConsumer<ResourceDeployment, AnActionEvent> exportParameterHandler = (c, e) -> DeploymentActions.exportParameters(c);
        am.registerHandler(ArmActionsContributor.EXPORT_PARAMETER, exportParameterCondition, exportParameterHandler);

        final BiPredicate<ResourceDeployment, AnActionEvent> exportTemplateCondition = (r, e) -> r instanceof ResourceDeployment;
        final BiConsumer<ResourceDeployment, AnActionEvent> exportTemplateHandler = (c, e) -> DeploymentActions.exportTemplate(c);
        am.registerHandler(ArmActionsContributor.EXPORT_TEMPLATE, exportTemplateCondition, exportTemplateHandler);
    }

    @Override
    public int getOrder() {
        return ArmActionsContributor.INITIALIZE_ORDER + 1;
    }
}
