/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.cognitiveservices;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

public class CognitiveServicesActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.cognitiveservices.service";
    public static final String ACCOUNT_ACTIONS = "actions.cognitiveservices.account";
    public static final String DEPLOYMENT_ACTIONS = "actions.cognitiveservices.deployment";
    public static final Action.Id<Subscription> ENABLE_OPENAI = Action.Id.of("user/openai.enable_openai.subscription");
    public static final Action.Id<AzureCognitiveServices> CREATE_ACCOUNT = Action.Id.of("user/openai.create_account");
    public static final Action.Id<CognitiveAccount> CREATE_DEPLOYMENT = CognitiveAccount.CREATE_DEPLOYMENT;
    public static final Action.Id<CognitiveAccount> COPY_PRIMARY_KEY = Action.Id.of("user/openai.copy_primary_key.account");
    public static final Action.Id<CognitiveAccount> OPEN_ACCOUNT_IN_PLAYGROUND = Action.Id.of("user/openai.open_playground.account");
    public static final Action.Id<CognitiveDeployment> OPEN_DEPLOYMENT_IN_PLAYGROUND = CognitiveDeployment.OPEN_DEPLOYMENT_IN_PLAYGROUND;
    public static final Action.Id<ResourceGroup> GROUP_CREATE_ACCOUNT = Action.Id.of("user/openai.create_account.group");
    public static final String ENABLE_OPENAI_URL = "https://aka.ms/oai/access";

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(CREATE_ACCOUNT)
            .withLabel("Create Azure OpenAI Service")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .withShortcut(am.getIDEDefaultShortcuts().add())
            .register(am);

        new Action<>(CREATE_DEPLOYMENT)
            .withLabel("Create New Deployment")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof CognitiveAccount)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(COPY_PRIMARY_KEY)
            .withLabel("Copy Primary Key")
            .withIcon(AzureIcons.Action.LOG.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof CognitiveAccount)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler(resource -> {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(resource.getPrimaryKey()), null);
                AzureMessager.getMessager().info("Primary key copied");
            })
            .register(am);

        new Action<>(OPEN_ACCOUNT_IN_PLAYGROUND)
            .withLabel("Open in AI Playground")
            .withIdParam(AbstractAzResource::getName)
            .withIcon(AzureIcons.CognitiveServices.PLAYGROUND.getIconPath())
            .visibleWhen(s -> s instanceof CognitiveAccount)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(OPEN_DEPLOYMENT_IN_PLAYGROUND)
            .withLabel("Open in AI Playground")
            .withIdParam(AbstractAzResource::getName)
            .withIcon(AzureIcons.CognitiveServices.PLAYGROUND.getIconPath())
            .visibleWhen(s -> s instanceof CognitiveDeployment && ((CognitiveDeployment) s).getModel().isGPTModel())
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(GROUP_CREATE_ACCOUNT)
            .withLabel("Azure OpenAI service")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(ENABLE_OPENAI)
                .withLabel("Azure OpenAI service")
                .withIdParam(s -> Optional.ofNullable(s).map(Subscription::getId).orElse(StringUtils.EMPTY))
                .withHandler(s -> {
                    Optional.ofNullable(s).ifPresent(subscription -> OperationContext.current().setTelemetryProperty("subscriptionId", subscription.getId()));
                    AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(ENABLE_OPENAI_URL);
                })
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            "---",
            ResourceCommonActionsContributor.GETTING_STARTED,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            CognitiveServicesActionsContributor.CREATE_ACCOUNT
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup accountActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            CognitiveServicesActionsContributor.COPY_PRIMARY_KEY,
            CognitiveServicesActionsContributor.OPEN_ACCOUNT_IN_PLAYGROUND,
            "---",
            CognitiveServicesActionsContributor.CREATE_DEPLOYMENT,
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(ACCOUNT_ACTIONS, accountActionGroup);

        final ActionGroup deploymentAction = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            CognitiveServicesActionsContributor.OPEN_DEPLOYMENT_IN_PLAYGROUND,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(DEPLOYMENT_ACTIONS, deploymentAction);


        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_ACCOUNT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
