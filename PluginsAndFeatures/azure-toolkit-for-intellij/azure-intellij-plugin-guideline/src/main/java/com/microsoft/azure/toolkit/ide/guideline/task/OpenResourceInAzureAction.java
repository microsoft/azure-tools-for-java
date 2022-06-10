package com.microsoft.azure.toolkit.ide.guideline.task;

import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;
import static com.microsoft.azure.toolkit.ide.guideline.task.create.webapp.CreateWebAppTask.RESOURCE_ID;

public class OpenResourceInAzureAction implements Task {
    @Override
    public InputComponent getInput() {
        return null;
    }

    @Override
    public void execute(Context context) throws Exception {
        final String id = (String) context.getProperty(RESOURCE_ID);
        final String url = "https://" + Azure.az(AzureWebApp.class).webApp(id).getHostName();
//        final ResourceId resourceId = ResourceId.fromString(id);
//        final IAccount account = Azure.az(IAzureAccount.class).account();
//        final Subscription subscription = account.getSubscription(resourceId.subscriptionId());
//        final String url = String.format("%s/#@%s/resource%s", account.portalUrl(), subscription.getTenantId(), id);
        AzureActionManager.getInstance().getAction(OPEN_URL).handle(url);
    }
}
