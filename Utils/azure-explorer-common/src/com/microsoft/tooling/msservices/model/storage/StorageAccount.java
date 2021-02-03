/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.model.storage;

import com.microsoft.azure.management.storage.AccessTier;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azure.management.storage.Kind;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class StorageAccount extends ClientStorageAccount {
    private String type = "";
    private String description = "";
    private String label = "";
    private String status = "";
    private String location = "";
    private String secondaryKey = "";
    private String managementUri = "";
    private String primaryRegion = "";
    private String primaryRegionStatus = "";
    private String secondaryRegion = "";
    private String secondaryRegionStatus = "";
    private Calendar lastFailover = new GregorianCalendar();

    private String resourceGroupName = "";
    private boolean isNewResourceGroup;
    private Kind kind;
    private AccessTier accessTier;

    public StorageAccount(@NotNull String name,
                          @NotNull String subscriptionId) {
        super(name);
        this.subscriptionId = subscriptionId;
    }

    @NotNull
    public String getType() {
        return type;
    }

    public void setType(@NotNull String type) {
        this.type = type;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    public void setLabel(@NotNull String label) {
        this.label = label;
    }

    @NotNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NotNull String status) {
        this.status = status;
    }

    @NotNull
    public String getLocation() {
        return location;
    }

    public void setLocation(@NotNull String location) {
        this.location = location;
    }

    @NotNull
    public String getSecondaryKey() {
        return secondaryKey;
    }

    public void setSecondaryKey(@NotNull String secondaryKey) {
        this.secondaryKey = secondaryKey;
    }

    @NotNull
    public String getManagementUri() {
        return managementUri;
    }

    public void setManagementUri(@NotNull String managementUri) {
        this.managementUri = managementUri;
    }

    @NotNull
    public String getPrimaryRegion() {
        return primaryRegion;
    }

    public void setPrimaryRegion(@NotNull String primaryRegion) {
        this.primaryRegion = primaryRegion;
    }

    @NotNull
    public String getPrimaryRegionStatus() {
        return primaryRegionStatus;
    }

    public void setPrimaryRegionStatus(@NotNull String primaryRegionStatus) {
        this.primaryRegionStatus = primaryRegionStatus;
    }

    @NotNull
    public String getSecondaryRegion() {
        return secondaryRegion;
    }

    public void setSecondaryRegion(@NotNull String secondaryRegion) {
        this.secondaryRegion = secondaryRegion;
    }

    @NotNull
    public String getSecondaryRegionStatus() {
        return secondaryRegionStatus;
    }

    public void setSecondaryRegionStatus(@NotNull String secondaryRegionStatus) {
        this.secondaryRegionStatus = secondaryRegionStatus;
    }

    @NotNull
    public Calendar getLastFailover() {
        return lastFailover;
    }

    public void setLastFailover(@NotNull Calendar lastFailover) {
        this.lastFailover = lastFailover;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public boolean isNewResourceGroup() {
        return isNewResourceGroup;
    }

    public void setNewResourceGroup(boolean newResourceGroup) {
        isNewResourceGroup = newResourceGroup;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public AccessTier getAccessTier() {
        return accessTier;
    }

    public void setAccessTier(AccessTier accessTier) {
        this.accessTier = accessTier;
    }
}
