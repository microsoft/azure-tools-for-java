/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.favorite;

import com.azure.core.http.rest.Page;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzModuleNode;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.IMachineStore;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Emulatable;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class Favorites extends AbstractAzResourceModule<Favorite, AzResource.None, AbstractAzResource<?, ?, ?>> {
    private static final String FAVORITE_ICON = AzureIcons.Common.FAVORITE.getIconPath();
    private static final String FAVORITE_GROUP = "{favorites_group}";
    public static final String NAME = "favorites";
    public static final String LOCAL = "local";
    @Getter
    private static final Favorites instance = new Favorites();
    private static final ObjectMapper mapper = new ObjectMapper();
    List<AbstractAzResource<?, ?, ?>> favorites = new LinkedList<>();

    private Favorites() {
        super(NAME, AzResource.NONE);
        AzureEventBus.on("account.logged_out.account", new AzureEventBus.EventListener((e) -> {
            this.clear();
            this.refresh();
        }));
        AzureEventBus.on("account.logged_in.account", new AzureEventBus.EventListener((e) -> this.refresh()));
    }

    @Override
    public synchronized void clear() {
        super.clear();
        this.favorites.clear();
    }

    @Nonnull
    @Override
    public synchronized List<Favorite> list() {
        final List<Favorite> result = new LinkedList<>(super.list());
        result.sort(Comparator.comparing(item -> ListUtils.indexOf(this.favorites, e -> StringUtils.equalsIgnoreCase(e.getId(), item.getId()))));
        return result;
    }

    @Nonnull
    @Override
    protected Iterator<? extends Page<AbstractAzResource<?, ?, ?>>> loadResourcePagesFromAzure() {
        final List<AbstractAzResource<?, ?, ?>> favoriteAzureResources = loadFavoriteAzureResources();
        final List<AbstractAzResource<?, ?, ?>> favoriteLocalResources = loadFavoriteLocalResources();
        this.favorites = ListUtils.union(favoriteAzureResources, favoriteLocalResources);
        return Collections.singletonList(new ItemPage<>(favorites.stream())).iterator();
    }

    protected List<AbstractAzResource<?, ?, ?>> loadFavoriteAzureResources() {
        final AzureAccount azureAccount = Azure.az(AzureAccount.class);
        return azureAccount.isLoggedIn() ? loadResource(Objects.requireNonNull(azureAccount.getAccount()).getUsername()) : Collections.emptyList();
    }

    protected List<AbstractAzResource<?, ?, ?>> loadFavoriteLocalResources() {
        return loadResource(LOCAL);
    }

    protected List<AbstractAzResource<?, ?, ?>> loadResource(final String key) {
        final IMachineStore store = AzureStoreManager.getInstance().getMachineStore();
        final String favorites = store.getProperty(this.getName(), key);
        if (StringUtils.isNotBlank(favorites)) {
            final ObjectMapper mapper = new ObjectMapper();
            try {
                return new LinkedList<>(Arrays.asList(mapper.readValue(favorites, String[].class))).stream()
                        .map(String::toLowerCase).distinct()
                        .map(id -> Azure.az().getById(id))
                        .filter(Objects::nonNull)
                        .map(c -> ((AbstractAzResource<?, ?, ?>) c)).collect(Collectors.toList());
            } catch (final JsonProcessingException ex) {
                AzureMessager.getMessager().warning(String.format("failed to load favorites with key %s.", key));
            }
        }
        return Collections.emptyList();
    }

    @Nullable
    @Override
    protected AbstractAzResource<?, ?, ?> loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return this.favorites.stream().filter(favorite -> StringUtils.equalsIgnoreCase(favorite.getId(), name)).findFirst()
                .orElse(null);
    }

    @Override
    @SneakyThrows
    @AzureOperation(name = "internal/favorite.delete_favorite")
    protected void deleteResourceFromAzure(@Nonnull String favoriteId) {
        final String resourceId = URLDecoder.decode(ResourceId.fromString(favoriteId).name(), StandardCharsets.UTF_8.name());
        this.favorites.removeIf(favorite -> StringUtils.equalsIgnoreCase(favorite.getId(), resourceId));
        this.persist();
    }

    @Nonnull
    @Override
    @SneakyThrows
    public String toResourceId(@Nonnull String resourceName, @Nullable String resourceGroup) {
        final String encoded = URLEncoder.encode(resourceName, StandardCharsets.UTF_8.name());
        final String template = "/subscriptions/%s/resourceGroups/%s/providers/AzureToolkits.Favorite/favorites/%s";
        return String.format(template, AzResource.NONE.getName(), AzResource.NONE.getName(), encoded);
    }

    @Nonnull
    @Override
    protected Favorite newResource(@Nonnull AbstractAzResource<?, ?, ?> remote) {
        return new Favorite(remote, this);
    }

    @Nonnull
    @Override
    protected Favorite newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        throw new AzureToolkitRuntimeException("not supported");
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

    public boolean exists(@Nonnull String resourceId) {
        return this.favorites.stream().anyMatch(favorite -> StringUtils.equalsIgnoreCase(favorite.getId(), resourceId));
    }


    @AzureOperation(name = "internal/favorite.unpin_all")
    public void unpinAll() {
        this.clear();
        this.persist();
        this.refresh();
    }

    @AzureOperation(name = "internal/favorite.add_favorite")
    public synchronized void pin(@Nonnull AbstractAzResource<?, ?, ?> resource) {
        if (this.exists(resource.getId())) {
            final String message = String.format("%s '%s' is already pinned.", resource.getResourceTypeName(), resource.getName());
            AzureMessager.getMessager().warning(message);
            return;
        }
        final FavoriteDraft draft = this.create(resource.getId(), FAVORITE_GROUP);
        draft.setResource(resource);
        draft.createIfNotExist();
    }

    public void unpin(@Nonnull String resourceId) {
        this.delete(resourceId, FAVORITE_GROUP);
    }

    public void persist() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            persistFavoritesLocalResources();
            persistFavoritesAzureResources();
        });
    }

    private void persistFavoritesAzureResources() {
        final AzureAccount azureAccount = Azure.az(AzureAccount.class);
        if (!azureAccount.isLoggedIn()) {
            return;
        }
        final String userName = Objects.requireNonNull(azureAccount.getAccount()).getUsername();
        final List<AbstractAzResource<?, ?, ?>> emulatorResources = this.favorites.stream()
                .filter(resource -> !(resource instanceof Emulatable && ((Emulatable) resource).isEmulatorResource()))
                .collect(Collectors.toList());
        persistFavorites(emulatorResources, userName);
    }

    private void persistFavoritesLocalResources() {
        final List<AbstractAzResource<?, ?, ?>> emulatorResources = this.favorites.stream()
                .filter(resource -> resource instanceof Emulatable && ((Emulatable) resource).isEmulatorResource())
                .collect(Collectors.toList());
        persistFavorites(emulatorResources, LOCAL);
    }

    private void persistFavorites(@Nonnull final List<AbstractAzResource<?, ?, ?>> resources, final String key) {
        try {
            final IMachineStore store = AzureStoreManager.getInstance().getMachineStore();
            final List<String> idList = resources.stream().map(AbstractAzResource::getId).distinct().collect(Collectors.toList());
            store.setProperty(this.getName(), key, mapper.writeValueAsString(idList));
        } catch (final JsonProcessingException e) {
            AzureMessager.getMessager().error("failed to persist favorites.");
        }
    }

    public static <T extends AzResource> Node<Favorites> buildFavoriteRoot(IExplorerNodeProvider.Manager manager) {
        final AzureActionManager am = AzureActionManager.getInstance();
        final AzureActionManager.Shortcuts shortcuts = am.getIDEDefaultShortcuts();

        final Action<Favorites> unpinAllAction = new Action<>(Action.Id.<Favorites>of("user/$resource.unpin_all"))
            .withLabel("Unmark All As Favorite")
            .withIcon(AzureIcons.Action.UNPIN.getIconPath())
            .visibleWhen(s -> s instanceof Favorites)
            .enableWhen(s -> !Favorites.getInstance().favorites.isEmpty())
            .withHandler(Favorites::unpinAll)
            .withShortcut("control F11");

        return new AzModuleNode<>(Favorites.getInstance())
            .withIcon(FAVORITE_ICON)
            .withLabel("Favorites")
            .withChildrenLoadLazily(false)
            .withActions(new ActionGroup(unpinAllAction, "---", ResourceCommonActionsContributor.REFRESH))
            .addChildren(Favorites::list, (o, parent) -> {
                final Node<?> node = manager.createNode(o.getResource(), parent, IExplorerNodeProvider.ViewType.APP_CENTRIC);
                if (node instanceof AzResourceNode) {
                    @SuppressWarnings("unchecked")
                    final Function<Object, String> descProvider = (Function<Object, String>) node.getDescBuilder();
                    ((AzResourceNode<?>) node).withTips(r -> {
                            if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                                return "";
                            }
                            final ResourceId id = ResourceId.fromString(r.getId());
                            final Subscription subs = r.getSubscription();
                            final String rg = id.resourceGroupName();
                            final String typeName = r.getModule().getResourceTypeName();
                            return String.format("Type:%s | Subscription: %s | Resource Group:%s", typeName, subs.getName(), rg) +
                                Optional.ofNullable(id.parent()).map(p -> " | Parent:" + p.name()).orElse("");
                        })
                        .withDescription(r -> {
                            if (!r.getSubscription().isSelected()) {
                                return String.format("(%s)", r.getSubscription().getName());
                            }
                            return Optional.ofNullable(descProvider).map(p -> p.apply(node.getValue())).orElse(null);
                        });
                }
                return node;
            });
    }

    public boolean isAuthRequiredForListing() {
        return false;
    }
}
