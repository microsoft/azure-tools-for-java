/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.identities.AzureManagedIdentity;
import com.microsoft.azure.toolkit.lib.identities.Identity;
import lombok.Getter;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.connector.IManagedIdentitySupported.checkPermission;

public class UserAssignedManagedIdentityComboBox extends AzureComboBox<Identity> {
    @Getter
    private AzureServiceResource<?> resource;
    @Getter
    private Subscription subscription;
    private final Map<Identity, Boolean> permissions = new HashMap<>();

    public UserAssignedManagedIdentityComboBox() {
        super();
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    public void setResource(AzureServiceResource<?> azResource) {
        if (Objects.equals(azResource, this.resource)) {
            return;
        }
        this.clear();
        this.resource = azResource;
        this.subscription = Optional.ofNullable(azResource.getData()).map(AzResource::getSubscription).orElse(null);
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Identity identity) {
            return Objects.isNull(resource) || BooleanUtils.isTrue(permissions.get(identity)) ?
                    identity.getName() : String.format("%s (No permission)", identity.getName());
        }
        return super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<Identity> loadItems() throws Exception {
        final List<Identity> identities = Objects.isNull(subscription) ?
                Azure.az(AzureManagedIdentity.class).identities() :
                Azure.az(AzureManagedIdentity.class).forSubscription(this.subscription.getId()).identity().list();
        if (Objects.isNull(resource)) {
            return identities;
        }
        permissions.clear();
        identities.forEach(identity -> permissions.put(identity, checkPermission(resource, identity.getPrincipalId())));
        identities.sort(Comparator.comparing(permissions::get).reversed());
        return identities;
    }
}
