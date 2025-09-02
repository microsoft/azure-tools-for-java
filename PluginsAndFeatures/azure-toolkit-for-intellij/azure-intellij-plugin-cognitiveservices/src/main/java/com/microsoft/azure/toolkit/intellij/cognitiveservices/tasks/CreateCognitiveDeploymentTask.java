/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.input.CognitiveDeploymentInput;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.*;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountSku;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentSku;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public class CreateCognitiveDeploymentTask implements Task {
    public static final String DEFAULT_COGNITIVE_DEPLOYMENT = "default_cognitive_deployment";
    public static final String GETTING_START_ACCOUNT = "getting-started-service";
    public static final String GETTING_START_DEPLOYMENT = "getting-started-deployment";
    public static final String ACCOUNT = "cognitive_account";
    public static final String DEPLOYMENT = "cognitive_deployment";
    private final ComponentContext context;

    public CreateCognitiveDeploymentTask(ComponentContext taskContext) {
        this.context = taskContext;
    }

    @Override
    public void prepare() {
        final Phase currentPhase = context.getCourse().getCurrentPhase();
        currentPhase.expandPhasePanel();
        this.init();
    }

    @Override
    public void execute() {
        final CognitiveAccount account = (CognitiveAccount) Objects.requireNonNull(context.getParameter(CognitiveDeploymentInput.COGNITIVE_ACCOUNT),
                "Please select your Azure OpenAI service in the combo box.");
        final CognitiveDeployment deployment = (CognitiveDeployment) Objects.requireNonNull(context.getParameter(CognitiveDeploymentInput.COGNITIVE_DEPLOYMENT),
                "Please select your Azure OpenAI deployment in the combo box.");
        if (!account.exists()) {
            ((CognitiveAccountDraft) account).commit();
        } else {
            AzureMessager.getMessager().info(String.format("Using existing Azure OpenAI service %s.", account.getName()));
        }
        if (!deployment.exists()) {
            final CognitiveDeploymentDraft draft = (CognitiveDeploymentDraft) deployment;
            final CognitiveDeploymentDraft.Config config = new CognitiveDeploymentDraft.Config();
            final AccountModel model = findBestAvailableGPTModel(account);
            config.setSku(DeploymentSku.fromModelSku(model.getSkus().get(0)));
            config.setModel(DeploymentModel.fromAccountModel(model));
            draft.setConfig(config);
            (draft).commit();
        } else {
            AzureMessager.getMessager().info(String.format("Using existing Azure OpenAI deployment %s.", deployment.getName()));
        }
        context.applyResult("resourceId", account.getId());
        context.applyResult(ACCOUNT, account);
        context.applyResult(DEPLOYMENT, deployment);
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.openai.create_deployment";
    }

    private void init() {
        final Subscription subscription = (Subscription) context.getParameter(SelectSubscriptionTask.SUBSCRIPTION);
        final CognitiveAccountModule module = Azure.az(AzureCognitiveServices.class).forSubscription(subscription.getId()).accounts();
        final CognitiveAccount cognitiveAccount = module.list().stream()
                .filter(account -> StringUtils.startsWith(account.getName(), GETTING_START_ACCOUNT))
                .findFirst().orElseGet(() -> createAccountDraft(subscription));
        final CognitiveDeployment draft = cognitiveAccount.deployments().list().stream()
                .filter(deployment -> StringUtils.startsWith(deployment.getName(), GETTING_START_DEPLOYMENT))
                .findFirst().orElseGet(() -> createDeploymentDraft(cognitiveAccount));
        context.applyResult(DEFAULT_COGNITIVE_DEPLOYMENT, draft);
    }

    protected CognitiveDeploymentDraft createDeploymentDraft(final CognitiveAccount account) {
        final String name = Utils.generateRandomResourceName(GETTING_START_DEPLOYMENT, 40);
        final CognitiveDeploymentDraft draft = account.deployments().create(name, account.getResourceGroupName());
        draft.setConfig(new CognitiveDeploymentDraft.Config());
        return draft;
    }

    protected CognitiveAccountDraft createAccountDraft(final Subscription subscription) {
        final String account = Utils.generateRandomResourceName(GETTING_START_ACCOUNT, 40);
        final String rgName = String.format("rg-%s", account);
        final ResourceGroup group = Azure.az(AzureResources.class).groups(subscription.getId()).create(rgName, rgName);
        final CognitiveAccountModule accounts = Azure.az(AzureCognitiveServices.class).accounts(subscription.getId());
        final AccountSku accountSku = accounts.listSkus(null).get(0);
        final List<Region> regions = accounts.listRegion(accountSku);
        final Region region = regions.contains(Region.US_EAST) ? Region.US_EAST : regions.get(0);
        CognitiveAccountDraft draft = accounts.create(account, rgName);
        draft.setConfig(CognitiveAccountDraft.Config.builder().resourceGroup(group).sku(accountSku).region(region).build());
        return draft;
    }

    /**
     * Find the best available GPT model, prioritizing newer models like gpt-4o-mini
     */
    private AccountModel findBestAvailableGPTModel(final CognitiveAccount account) {
        final List<AccountModel> allModels = account.listModels();
        
        // Define preferred models in order of preference (most preferred first)
        final String[] preferredModels = {
            "gpt-4o-mini",     // Cost-effective, widely available
            "gpt-4o",          // Latest capabilities  
            "gpt-35-turbo",    // Widely available fallback
            "gpt-4-turbo",     // Alternative advanced model
            "gpt-4"            // Fallback GPT-4
        };
        
        // First, try to find models by exact name match
        for (String preferredModelName : preferredModels) {
            for (AccountModel model : allModels) {
                if (model.getName().toLowerCase().contains(preferredModelName.toLowerCase()) && 
                    hasValidSkus(model)) {
                    AzureMessager.getMessager().info(String.format("Using preferred model: %s (version: %s)", 
                        model.getName(), model.getVersion()));
                    return model;
                }
            }
        }
        
        // Fallback: use the original GPT filter
        final AccountModel fallbackModel = allModels.stream()
            .filter(AccountModel::isGPTModel)
            .filter(this::hasValidSkus)
            .findFirst()
            .orElse(null);
            
        if (fallbackModel != null) {
            AzureMessager.getMessager().warning(String.format("Using fallback GPT model: %s (version: %s). " +
                "Consider updating to a newer model like gpt-4o-mini for better performance and cost.", 
                fallbackModel.getName(), fallbackModel.getVersion()));
            return fallbackModel;
        }
        
        // If no GPT models found, throw exception
        throw new AzureToolkitRuntimeException(String.format(
            "No suitable GPT model found in service %s. Available models: %s. " +
            "Please ensure your Azure OpenAI service has access to GPT models like gpt-4o-mini.", 
            account.getName(), 
            allModels.stream().map(AccountModel::getName).reduce((a, b) -> a + ", " + b).orElse("none")));
    }

    /**
     * Check if the model has valid SKUs for deployment
     */
    private boolean hasValidSkus(AccountModel model) {
        return model.getSkus() != null && !model.getSkus().isEmpty();
    }
}
