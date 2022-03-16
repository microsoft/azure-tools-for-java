/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.favorite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzureModuleLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.IMachineStore;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
public class Favorites extends AbstractAzResourceModule<Favorite, AzResource.None, AbstractAzResource<?, ?, ?>> {
    private static final String FAVORITE_ICON = "/icons/Common/favorite.svg";

    @Getter
    private static final Favorites instance = new Favorites();
    public static final String NAME = "toolkitFavorites";
    List<String> favorites = new LinkedList<>();

    private Favorites() {
        super(NAME, AzResource.NONE);
        AzureEventBus.on("account.logout.account", (e) -> {
            this.clear();
            this.refresh();
        });
        AzureEventBus.on("account.login.account", (e) -> this.refresh());
    }

    @Override
    public synchronized void clear() {
        super.clear();
        this.favorites.clear();
    }

    @Nonnull
    @Override
    public synchronized List<Favorite> list() {
        if (!Azure.az(AzureAccount.class).isSignedIn()) {
            return Collections.emptyList();
        }
        final List<Favorite> result = new LinkedList<>(super.list());
        result.sort(Comparator.comparing(item -> this.favorites.indexOf(item.getName())));
        return result;
    }

    @Nonnull
    @Override
    protected Stream<AbstractAzResource<?, ?, ?>> loadResourcesFromAzure() {
        final Account account = Azure.az(AzureAccount.class).account();
        final String user = account.getEntity().getEmail();
        final IMachineStore store = AzureStoreManager.getInstance().getMachineStore();
        final String favorites = store.getProperty(this.getName(), user);
        if (StringUtils.isNotBlank(favorites)) {
            final ObjectMapper mapper = new ObjectMapper();
            try {
                this.favorites = new LinkedList<>(Arrays.asList(mapper.readValue(favorites, String[].class)));
            } catch (final JsonProcessingException ex) {
                AzureMessager.getMessager().error("failed to load favorites.");
                this.favorites = new LinkedList<>();
            }
        }
        return this.favorites.stream().map(id -> Azure.az().getById(id)).filter(Objects::nonNull)
            .map(c -> ((AbstractAzResource<?, ?, ?>) c));
    }

    @Nullable
    @Override
    protected AbstractAzResource<?, ?, ?> loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        if (this.favorites.contains(name)) {
            return Azure.az().getById(name);
        }
        return null;
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        this.favorites.remove(resourceId.substring("$NONE$/toolkitFavorites/".length()));
        this.persist();
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

    public boolean exists(@Nonnull String resourceId) {
        return this.favorites.contains(resourceId);
    }

    public void unpinAll() {
        this.clear();
        this.persist();
        this.refresh();
    }

    public void pin(@Nonnull String resourceId) {
        this.create(resourceId, null).commit();
    }

    public void unpin(@Nonnull String resourceId) {
        this.delete(resourceId, null);
    }

    public void persist() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final IMachineStore store = AzureStoreManager.getInstance().getMachineStore();
            final Account account = Azure.az(AzureAccount.class).account();
            final String user = account.getEntity().getEmail();
            final ObjectMapper mapper = new ObjectMapper();
            try {
                store.setProperty(this.getName(), user, mapper.writeValueAsString(this.favorites));
            } catch (final JsonProcessingException e) {
                AzureMessager.getMessager().error("failed to persist favorites.");
            }
        });
    }

    public static Node<Favorites> buildFavoriteRoot(IExplorerNodeProvider.Manager manager) {
        final AzureActionManager.Shortcuts shortcuts = AzureActionManager.getInstance().getIDEDefaultShortcuts();

        final ActionView.Builder unpinAllView = new ActionView.Builder("Unmark All As Favorite", "/icons/Common/unpin.svg")
            .enabled(s -> s instanceof Favorites);
        final Consumer<Favorites> unpinAllHandler = Favorites::unpinAll;
        final Action<Favorites> unpinAllAction = new Action<>(unpinAllHandler, unpinAllView);
        unpinAllAction.setShortcuts("control F11");

        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
            .enabled(s -> s instanceof Favorites);
        final Consumer<Favorites> refreshHandler = Favorites::refresh;
        final Action<Favorites> refreshAction = new Action<>(refreshHandler, refreshView);
        refreshAction.setShortcuts(shortcuts.refresh());

        final AzureModuleLabelView<Favorites> rootView = new AzureModuleLabelView<>(Favorites.getInstance(), "Favorites", FAVORITE_ICON);
        return new Node<>(Favorites.getInstance(), rootView).lazy(false)
            .actions(new ActionGroup(unpinAllAction, "---", refreshAction))
            .addChildren(Favorites::list, (o, parent) -> {
                final Node<?> node = manager.createNode(o.getResource(), parent);
                if (Objects.nonNull(node) && node.view() instanceof AzureResourceLabelView) {
                    node.view(new FavoriteNodeView((AzureResourceLabelView<?>) node.view()));
                } else if (Objects.isNull(node)) {
                    throw new AzureToolkitRuntimeException("failed to render Favorite node from " + o.getResource());
                }
                return node;
            });
    }
}
