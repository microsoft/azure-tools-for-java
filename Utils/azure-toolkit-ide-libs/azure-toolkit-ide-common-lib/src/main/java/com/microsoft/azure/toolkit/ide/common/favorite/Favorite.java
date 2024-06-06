/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.favorite;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Favorite extends AbstractAzResource<Favorite, AzResource.None, AbstractAzResource<?, ?, ?>> {
    public static final String RG = "FAVORITES_RESOURCE_GROUP";

    protected Favorite(@Nonnull String resourceId, @Nonnull Favorites module) {
        super(resourceId, ResourceId.fromString(resourceId).resourceGroupName(), module);
    }

    @Override
    public void invalidateCache() {
        Optional.ofNullable(this.getResource()).ifPresent(AbstractAzResource::invalidateCache);
        super.invalidateCache();
    }

    /**
     * copy constructor
     */
    protected Favorite(@Nonnull Favorite origin) {
        super(origin);
    }

    protected Favorite(@Nonnull AbstractAzResource<?, ?, ?> remote, @Nonnull Favorites module) {
        // favorite's name is exactly the id of the resource.
        super(remote.getId(), remote.getResourceGroupName(), module);
    }

    @Override
    public boolean exists() {
        return this.remoteOptional().isPresent();
    }

    @Nullable
    public AbstractAzResource<?, ?, ?> getResource() {
        return this.getRemote();
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(AbstractAzResource<?, ?, ?> remote) {
        return Optional.of(remote).filter(AbstractAzResource::exists).map(r -> Status.RUNNING).orElse(Status.DELETED);
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }
}
