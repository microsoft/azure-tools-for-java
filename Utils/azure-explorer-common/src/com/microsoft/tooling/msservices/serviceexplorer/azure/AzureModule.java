/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure;

import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.enums.ErrorEnum;
import com.microsoft.azuretools.exception.AzureRuntimeException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;
import lombok.Setter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureModule extends AzureRefreshableNode {
    private static final String AZURE_SERVICE_MODULE_ID = AzureModule.class.getName();
    private static final String BASE_MODULE_NAME = "Azure";
    private static final String MODULE_NAME_NO_SUBSCRIPTION = "No subscription";
    private static final String ERROR_GETTING_SUBSCRIPTIONS_TITLE = "MS Services - Error Getting Subscriptions";
    private static final String ERROR_GETTING_SUBSCRIPTIONS_MESSAGE = "An error occurred while getting the subscription" +
            " list.\n(Message from Azure:%s)";

    @Nullable
    private Object project;
    @NotNull
    private VMArmModule vmArmServiceModule;
    @NotNull
    private RedisCacheModule redisCacheModule;
    @NotNull
    private StorageModule storageModule;
    @NotNull
    private WebAppModule webAppModule;
    @Nullable
    private HDInsightRootModule hdInsightModule;
    @Nullable
    private HDInsightRootModule sparkServerlessClusterRootModule;
    @Nullable
    private HDInsightRootModule arcadiaModule;
    @NotNull
    private ContainerRegistryModule containerRegistryModule;
    @NotNull
    private FunctionModule functionModule;

    /**
     * Constructor.
     *
     * @param project project
     */
    public AzureModule(@Nullable Object project) {
        super(AZURE_SERVICE_MODULE_ID, composeName(), null, null);
        this.project = project;
        storageModule = new StorageModule(this);
        webAppModule = new WebAppModule(this);
        //hdInsightModule = new HDInsightRootModule(this);
        vmArmServiceModule = new VMArmModule(this);
        redisCacheModule = new RedisCacheModule(this);
        containerRegistryModule = new ContainerRegistryModule(this);
        functionModule = new FunctionModule(this);
        try {
            SignInOutListener signInOutListener = new SignInOutListener();
            AuthMethodManager.getInstance().addSignInEventListener(signInOutListener);
            AuthMethodManager.getInstance().addSignOutEventListener(signInOutListener);
            signInOutListener.run();
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
        // in case we already signed in with service principal between restarts, sign in event was not fired
        addSubscriptionSelectionListener();
    }

    private static String composeName() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            if (AuthMethodManager.getInstance().isRestoringSignIn()) {
                return BASE_MODULE_NAME + " (Signing In...)";
            }
            // not signed in
            if (azureManager == null) {
                return BASE_MODULE_NAME + " (Not Signed In)";
            }
            SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
            List<SubscriptionDetail> subscriptionDetails = subscriptionManager.getSubscriptionDetails();
            List<SubscriptionDetail> selectedSubscriptions = subscriptionDetails.stream()
                    .filter(SubscriptionDetail::isSelected).collect(Collectors.toList());
            return String.format("%s (%s)", BASE_MODULE_NAME, getAccountDescription(selectedSubscriptions));

        } catch (AzureRuntimeException e) {
            DefaultLoader.getUIHelper().showInfoNotification(
                    ERROR_GETTING_SUBSCRIPTIONS_TITLE, ErrorEnum.getDisplayMessageByCode(e.getCode()));
        } catch (Exception e) {
            final String msg = String.format(ERROR_GETTING_SUBSCRIPTIONS_MESSAGE, e.getMessage());
            DefaultLoader.getUIHelper().showException(msg, e, ERROR_GETTING_SUBSCRIPTIONS_TITLE, false, true);
        }
        return BASE_MODULE_NAME;
    }

    public void setHdInsightModule(@NotNull HDInsightRootModule rootModule) {
        this.hdInsightModule = rootModule;
    }

    public void setSparkServerlessModule(@NotNull HDInsightRootModule rootModule) {
        this.sparkServerlessClusterRootModule = rootModule;
    }

    public void setArcadiaModule(@NotNull HDInsightRootModule rootModule) {
        this.arcadiaModule = rootModule;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // add the module; we check if the node has
        // already been added first because this method can be called
        // multiple times when the user clicks the "Refresh" context
        // menu item
        if (!isDirectChild(vmArmServiceModule)) {
            addChildNode(vmArmServiceModule);
        }
        if (!isDirectChild(redisCacheModule)) {
            addChildNode(redisCacheModule);
        }
        if (!isDirectChild(storageModule)) {
            addChildNode(storageModule);
        }
        if (!isDirectChild(webAppModule)) {
            addChildNode(webAppModule);
        }
        if (!isDirectChild(functionModule)) {
            addChildNode(functionModule);
        }
        if (hdInsightModule != null && !isDirectChild(hdInsightModule)) {
            addChildNode(hdInsightModule);
        }

        if (sparkServerlessClusterRootModule != null &&
                sparkServerlessClusterRootModule.isFeatureEnabled() &&
                !isDirectChild(sparkServerlessClusterRootModule)) {
            addChildNode(sparkServerlessClusterRootModule);
        }

        if (arcadiaModule != null && arcadiaModule.isFeatureEnabled() && !isDirectChild(arcadiaModule)) {
            addChildNode(arcadiaModule);
        }

        if (!isDirectChild(containerRegistryModule)) {
            addChildNode(containerRegistryModule);
        }
    }

    @Override
    protected void refreshFromAzure() throws AzureCmdException {
        try {
            if (AuthMethodManager.getInstance().isSignedIn() && hasSubscription()) {
                vmArmServiceModule.load(true);
                redisCacheModule.load(true);
                storageModule.load(true);
                webAppModule.load(true);
                functionModule.load(true);

                if (hdInsightModule != null) {
                    hdInsightModule.load(true);
                }

                if (sparkServerlessClusterRootModule != null) {
                    sparkServerlessClusterRootModule.load(true);
                }

                if (arcadiaModule != null && arcadiaModule.isFeatureEnabled()) {
                    arcadiaModule.load(true);
                }

                containerRegistryModule.load(true);
            }
        } catch (Exception e) {
            throw new AzureCmdException("Error loading Azure Explorer modules", e);
        }
    }

    @Nullable
    @Override
    public Object getProject() {
        return project;
    }

    private void addSubscriptionSelectionListener() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null || !hasSubscription()) {
                return;
            }
            azureManager.getSubscriptionManager().addListener(isRefresh -> {
                if (!isRefresh) {
                    handleSubscriptionChange();
                }
            });
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
    }

    private void handleSubscriptionChange() {
        setName(composeName());
        for (Node child : getChildNodes()) {
            child.removeAllChildNodes();
        }
        Optional.ofNullable(this.clearResourcesListener).ifPresent(Runnable::run);
    }

    @Setter
    private Runnable clearResourcesListener;

    private class SignInOutListener implements Runnable {
        @Override
        public void run() {
            handleSubscriptionChange();
            addSubscriptionSelectionListener();
        }
    }

    private static String getAccountDescription(List<SubscriptionDetail> selectedSubscriptions) {
        final int subsCount = selectedSubscriptions.size();
        switch (subsCount) {
            case 0:
                return MODULE_NAME_NO_SUBSCRIPTION;
            case 1:
                return selectedSubscriptions.get(0).getSubscriptionName();
            default:
                return String.format("%d subscriptions", selectedSubscriptions.size());
        }
    }

    private boolean hasSubscription() {
        return !this.name.contains(MODULE_NAME_NO_SUBSCRIPTION);
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.Common.AZURE;
    }
}
