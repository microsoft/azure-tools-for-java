/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.favorite;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class Favorite extends AbstractAzResource<Favorite, AzResource.None, AbstractAzResource<?, ?, ?>> {
    public static final String RG = "FAVORITES_RESOURCE_GROUP";

    protected Favorite(@Nonnull String resourceId, @Nonnull Favorites module) {
        super(resourceId, ResourceId.fromString(resourceId).resourceGroupName(), module);
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
        this.setRemote(remote);
    }

    public AbstractAzResource<?, ?, ?> getResource() {
        return this.getRemote();
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, Favorite, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull AbstractAzResource<?, ?, ?> remote) {
        return remote.exists() ? Status.RUNNING : Status.DELETED;
    }
}
