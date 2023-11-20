/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.common.component;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AzureResourceIconProvider<T extends AzResource> implements AzureIconProvider<T> {

    public static final AzureResourceIconProvider<AzResource> DEFAULT_AZURE_RESOURCE_ICON_PROVIDER = new AzureResourceIconProvider<>();

    private final List<Function<T, AzureIcon.Modifier>> modifierFunctionList = new ArrayList<>();

    public AzureResourceIconProvider() {
        withModifier(AzureResourceIconProvider::getStatusModifier);
    }

    public AzureResourceIconProvider<T> withModifier(@Nonnull Function<T, AzureIcon.Modifier> modifierFunction) {
        this.modifierFunctionList.add(modifierFunction);
        return this;
    }

    @Nullable
    @Override
    public AzureIcon getIcon(@Nullable T resource) {
        if (Objects.isNull(resource)) {
            return null;
        }
        final AzResource.FormalStatus formalStatus = resource.getFormalStatus();
        if (formalStatus.isWaiting()) {
            return AzureIcons.Common.REFRESH_ICON;
        }
        final String iconPath = getResourceBaseIconPath(resource);
        final List<AzureIcon.Modifier> modifiers = modifierFunctionList.stream()
                .map(function -> function.apply(resource))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return AzureIcon.builder().iconPath(iconPath).modifierList(modifiers).build();
    }

    public static <T extends AzResource> String getResourceIconPath(T resource) {
        final String modulePath = resource.getFullResourceType();
        return String.format("/icons/%s/default.svg", modulePath);
    }

    public static <T extends AzResource> String getResourceIconPath(ResourceId id) {
        final String modulePath = id.fullResourceType();
        return String.format("/icons/%s/default.svg", modulePath);
    }

    public static <T extends AzResource> String getResourceBaseIconPath(@Nonnull T resource) {
        return AzureResourceIconProvider.getResourceIconPath((AzResource) resource);
    }

    @Nullable
    public static <T extends AzResource> AzureIcon.Modifier getStatusModifier(T resource) {
        final AzResource.FormalStatus formalStatus = resource.getFormalStatus();
        if (formalStatus.isWaiting()) {
            return null;
        } else if (formalStatus.isRunning()) {
            return AzureIcon.Modifier.RUNNING;
        } else if (formalStatus.isFailed()) {
            return AzureIcon.Modifier.FAILED;
        } else if (formalStatus.isStopped()) {
            return AzureIcon.Modifier.STOPPED;
        } else {
            return AzureIcon.Modifier.UNKNOWN;
        }
    }

}
