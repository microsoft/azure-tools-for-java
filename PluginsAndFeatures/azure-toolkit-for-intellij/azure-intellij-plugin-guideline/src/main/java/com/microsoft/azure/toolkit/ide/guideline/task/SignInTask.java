package com.microsoft.azure.toolkit.ide.guideline.task;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.Step;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;

public class SignInTask implements Task {

    private Project project;

    public SignInTask(Project project) {
        this.project = project;
    }

    @Override
    public Step create(Process process) {
        return null;
    }

    @Override
    public InputComponent getInputComponent() {
        return null;
    }

    @Override
    public void execute(Context context) {
        IdentityAzureManager.getInstance().signInAzureCli().block();
        final AzureAccount az = Azure.az(AzureAccount.class);
        if (!az.isSignedIn() || CollectionUtils.isEmpty(az.getSubscriptions())) {
            AzureMessager.getMessager().warning("Failed to auth with azure cli, please make sure you have already signed in Azure CLI with subscription");
        } else {
            final Subscription subscription = az.getSubscriptions().get(0);
            az.account().selectSubscription(Arrays.asList(subscription.getId()));
            AzureMessager.getMessager().info(AzureString.format("Sign in successfully with subscription %s", subscription.getId()));
        }
    }

    @Override
    public void executeWithUI(Context context) {
        final AnAction action = ActionManager.getInstance().getAction("AzureToolkit.AzureSignIn");
        final DataContext dataContext = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
        AzureTaskManager.getInstance().runLater(() -> ActionUtil.invokeAction(action, dataContext, "AzurePluginStartupActivity", null, null));
    }
}
