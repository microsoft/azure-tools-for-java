/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.container;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistryDraft;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class ContainerRegistryMvpModel {

    private static final String ADMIN_USER_NOT_ENABLED = "Admin user is not enabled.";
    private static final String IMAGE_TAG = "image:tag";

    private ContainerRegistryMvpModel() {
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
    public List<ContainerRegistry> listContainerRegistries() {
        return az(AzureContainerRegistry.class).list().stream().flatMap(manager -> manager.registry().list().stream()).collect(Collectors.toList());
    }

    /**
     * Get Registry by subscription id.
     */
    public List<ContainerRegistry> listRegistryBySubscriptionId(@Nonnull String sid) {
        return az(AzureContainerRegistry.class).registry(sid).list();
    }

    /**
     * Get Registry Credential.
     */
    public PrivateRegistryImageSetting createImageSettingWithRegistry(@Nonnull final ContainerRegistry registry) throws AzureToolkitRuntimeException {
        if (!registry.isAdminUserEnabled()) {
            throw new AzureToolkitRuntimeException(ADMIN_USER_NOT_ENABLED);
        }
        return new PrivateRegistryImageSetting(registry.getLoginServerUrl(), registry.getUserName(), registry.getPrimaryCredential(), IMAGE_TAG, null);
    }

    /**
     * Get ACR by Id.
     */
    @Nonnull
    public ContainerRegistry getContainerRegistry(String sid, String id) {
        return az(AzureContainerRegistry.class).registry(sid).get(id);
    }

    /**
     * Set AdminUser enabled status of container registry.
     */
    public ContainerRegistry setAdminUserEnabled(String sid, String id, boolean enabled) {
        ContainerRegistry containerRegistry = getContainerRegistry(sid, id);
        ContainerRegistryDraft update = (ContainerRegistryDraft) containerRegistry.update();
        update.setAdminUserEnabled(enabled);
        return update.commit();
    }
}
