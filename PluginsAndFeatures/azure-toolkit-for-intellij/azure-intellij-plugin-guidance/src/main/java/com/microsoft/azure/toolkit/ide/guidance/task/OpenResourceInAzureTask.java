package com.microsoft.azure.toolkit.ide.guidance.task;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;

public class OpenResourceInAzureTask implements Task {
    private final ComponentContext taskContext;

    public OpenResourceInAzureTask(@Nonnull ComponentContext taskContext) {
        this.taskContext = taskContext;
    }

    @Override
    @AzureOperation(name = "guidance.open_resource_in_azure", type = AzureOperation.Type.SERVICE)
    public void execute() {
        final String id = (String) taskContext.getParameter("webappId");
        final ResourceId resourceId = ResourceId.fromString(id);
        final IAccount account = Azure.az(IAzureAccount.class).account();
        final Subscription subscription = account.getSubscription(resourceId.subscriptionId());
        final String url = String.format("%s/#@%s/resource%s", account.getPortalUrl(), subscription.getTenantId(), id);
        AzureActionManager.getInstance().getAction(OPEN_URL).handle(url);
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.common.open_portal";
    }
}
