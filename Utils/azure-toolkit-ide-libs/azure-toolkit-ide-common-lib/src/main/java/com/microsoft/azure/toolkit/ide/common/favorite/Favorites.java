/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.favorite;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class Favorites extends AbstractAzResourceModule<Favorite, AzResource.None, AbstractAzResource<?, ?, ?>> {
    @Getter
    private static final Favorites instance = new Favorites();

    // TODO: use platform storage API instead.
    public static final List<String> storage = new LinkedList<>(Arrays.asList(
        "/subscriptions/685ba005-af8d-4b04-8f16-a7bf38b2eb5a/resourceGroups/wangmi-rg-test/" +
            "providers/Microsoft.AppPlatform/Spring/wangmi-asc-basic/apps/springcloud-app-not-exist",
        "/subscriptions/685ba005-af8d-4b04-8f16-a7bf38b2eb5a/resourceGroups/wangmi-rg-test/" +
            "providers/Microsoft.AppPlatform/Spring/wangmi-asc-basic/apps/springcloud-app-20220308235135",
        "/subscriptions/685ba005-af8d-4b04-8f16-a7bf38b2eb5a/resourceGroups/wangmi-rg-test/" +
            "providers/Microsoft.AppPlatform/Spring/wangmi-asc-basic/apps/springcloud-app-20220308214445",
        "/subscriptions/685ba005-af8d-4b04-8f16-a7bf38b2eb5a/resourceGroups/wangmi-rg-test/" +
            "providers/Microsoft.AppPlatform/Spring/wangmi-asc-basic"
    ));

    private Favorites() {
        super("toolkitFavorites", AzResource.NONE);
    }

    @Nonnull
    @Override
    public synchronized List<Favorite> list() {
        final List<Favorite> result = new LinkedList<>(super.list());
        result.sort(Comparator.comparing(item -> storage.indexOf(item.getName())));
        return result;
    }

    @Nonnull
    @Override
    protected Stream<AbstractAzResource<?, ?, ?>> loadResourcesFromAzure() {
        return storage.stream().map(id -> Azure.az().getOrDraftById(id)).filter(Objects::nonNull)
            .map(c -> ((AbstractAzResource<?, ?, ?>) c));
    }

    @Nullable
    @Override
    protected AbstractAzResource<?, ?, ?> loadResourceFromAzure(@Nonnull String resourceId, @Nullable String resourceGroup) {
        if (storage.contains(resourceId)) {
            return Azure.az().getOrDraftById(resourceId);
        }
        return null;
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        storage.remove(resourceId);
    }

    @Nonnull
    @Override
    protected Favorite newResource(@Nonnull AbstractAzResource<?, ?, ?> remote) {
        return new Favorite(remote, this);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<Favorite, AbstractAzResource<?, ?, ?>> newDraftForCreate(@Nonnull String name, @Nullable String resourceGroup) {
        return new FavoriteDraft(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Favorites";
    }
}
