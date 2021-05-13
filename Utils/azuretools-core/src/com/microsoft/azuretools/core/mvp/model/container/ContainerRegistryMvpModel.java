/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.container;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.AccessKeyType;
import com.microsoft.azure.management.containerregistry.Registries;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class ContainerRegistryMvpModel {

    private final Map<String, List<ResourceEx<Registry>>> subscriptionIdToRegistryMap;

    private static final String CANNOT_GET_REGISTRY = "Cannot get Registry with resource Id: ";
    private static final String CANNOT_GET_CREDENTIAL = "Cannot get credential.";
    private static final String ADMIN_USER_NOT_ENABLED = "Admin user is not enabled.";
    private static final String IMAGE_TAG = "image:tag";

    private ContainerRegistryMvpModel() {
        subscriptionIdToRegistryMap = new ConcurrentHashMap<>();
    }

    private static final class SingletonHolder {
        private static final ContainerRegistryMvpModel INSTANCE = new ContainerRegistryMvpModel();
    }

    public static ContainerRegistryMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Get Registry instances mapped by Subscription id.
     */
    public List<ResourceEx<Registry>> listContainerRegistries(boolean force) {
        List<ResourceEx<Registry>> registryList = new ArrayList<>();
        List<Subscription> subscriptions = az(AzureAccount.class).account().getSelectedSubscriptions();
        for (Subscription sub : subscriptions) {
            registryList.addAll(listRegistryBySubscriptionId(sub.getId(), force));
        }
        return registryList;
    }

    /**
     * Get Registry by subscription id.
     */
    public List<ResourceEx<Registry>> listRegistryBySubscriptionId(@NotNull String sid, boolean force) {
        if (!force && subscriptionIdToRegistryMap.containsKey(sid)) {
            return subscriptionIdToRegistryMap.get(sid);
        }
        List<ResourceEx<Registry>> registryList = new ArrayList<>();
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        for (Registry registry: azure.containerRegistries().list()) {
            registryList.add(new ResourceEx<>(registry, sid));
        }
        subscriptionIdToRegistryMap.put(sid, registryList);
        return registryList;
    }

    /**
     * Get Registry Credential.
     */
    public PrivateRegistryImageSetting createImageSettingWithRegistry(@NotNull final Registry registry) throws Exception {
        if (!registry.adminUserEnabled()) {
            throw new Exception(ADMIN_USER_NOT_ENABLED);
        }
        final RegistryCredentials credentials = registry.getCredentials();
        if (credentials == null) {
            throw new Exception(CANNOT_GET_CREDENTIAL);
        }
        String username = credentials.username();
        final Map<AccessKeyType, String> passwords = credentials.accessKeys();
        if (Utils.isEmptyString(username) || passwords == null || !passwords.containsKey(AccessKeyType.PRIMARY)) {
            throw new Exception(CANNOT_GET_CREDENTIAL);
        }
        return new PrivateRegistryImageSetting(registry.loginServerUrl(), username, passwords.get(AccessKeyType.PRIMARY), IMAGE_TAG,
                null);
    }

    /**
     * Get ACR by Id.
     */
    @NotNull
    public Registry getContainerRegistry(String sid, String id) throws Exception {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        Registries registries = azure.containerRegistries();
        if (registries == null) {
            throw new Exception(CANNOT_GET_REGISTRY + id);
        }
        Registry registry = registries.getById(id);
        if (registry == null) {
            throw new Exception(CANNOT_GET_REGISTRY + id);
        }
        return registry;
    }

    /**
     * Set AdminUser enabled status of container registry.
     */
    public Registry setAdminUserEnabled(String sid, String id, boolean enabled) {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        Registries registries = azure.containerRegistries();
        if (registries != null) {
            Registry registry = registries.getById(id);
            if (registry != null) {
                clearTags(registry);
                if (enabled) {
                    registry.update().withRegistryNameAsAdminUser().apply();
                } else {
                    registry.update().withoutRegistryNameAsAdminUser().apply();
                }
            }
            return registry;
        } else {
            return null;
        }
    }

    /**
     * Work Around: see {@link com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel#clearTags(Object)}.
     */
    private void clearTags(@NotNull final Registry registry) {
        registry.inner().withTags(null);
    }

    private void clearRegistryMap() {
        subscriptionIdToRegistryMap.clear();
    }
}
