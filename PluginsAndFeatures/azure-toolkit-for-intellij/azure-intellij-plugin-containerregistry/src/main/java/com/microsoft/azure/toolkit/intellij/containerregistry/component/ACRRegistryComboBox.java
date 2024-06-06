/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistryServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ACRRegistryComboBox extends AzureComboBox<ContainerRegistry> {
    @Nullable
    private Subscription subscription;
    @Getter
    private final List<ContainerRegistry> draftItems = new LinkedList<>();

    @Override
    public String getLabel() {
        return "Registry";
    }

    @Override
    protected String getItemText(Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }

        final ContainerRegistry entity = (ContainerRegistry) item;
        if (entity.isDraftForCreating()) {
            return "(New) " + entity.getName();
        }
        return entity.isAdminUserEnabled() ? entity.getName() : String.format("%s (Admin User Disabled)", entity.getName());
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

    @Override
    public void setValue(@Nullable ContainerRegistry val, Boolean fixed) {
        if (Objects.nonNull(val) && val.isDraftForCreating() && !val.exists()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nullable
    @Override
    protected ContainerRegistry doGetDefaultValue() {
        return CacheManager.getUsageHistory(ContainerRegistry.class)
            .peek(g -> Objects.isNull(subscription) || Objects.equals(subscription.getId(), g.getSubscriptionId()));
    }

    @Nonnull
    @Override
    protected List<? extends ContainerRegistry> loadItems() {
        Stream<AzureContainerRegistryServiceSubscription> stream = Azure.az(AzureContainerRegistry.class).list().stream();
        if (Objects.nonNull(this.subscription)) {
            stream = stream.filter(s -> s.getSubscriptionId().equalsIgnoreCase(this.subscription.getId()));
        }
        final List<ContainerRegistry> remoteRegistries = stream.flatMap(s -> s.registry().list().stream())
            .sorted(Comparator.comparing(ContainerRegistry::getName)).toList();
        final List<ContainerRegistry> registries = new ArrayList<>(remoteRegistries);
        if (CollectionUtils.isNotEmpty(this.draftItems)) {
            this.draftItems.stream()
                .filter(i -> StringUtils.equalsIgnoreCase(this.subscription.getId(), i.getSubscriptionId()))
                .filter(i -> !remoteRegistries.contains(i)) // filter out the draft item which has been created
                .forEach(registries::add);
        }
        return registries;
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final List<ExtendableTextComponent.Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Create Azure Container Registry in Azure Portal (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.Add, tooltip,
                ()-> AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.CREATE_IN_PORTAL).handle(Azure.az(AzureContainerRegistry.class)));
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureContainerRegistry.class).registry(s.getId()).refresh());
        super.refreshItems();
    }
}
