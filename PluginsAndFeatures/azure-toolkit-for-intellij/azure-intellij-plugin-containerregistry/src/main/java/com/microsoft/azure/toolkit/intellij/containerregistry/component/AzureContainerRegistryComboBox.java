/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AzureContainerRegistryComboBox extends AzureComboBox<ContainerRegistry> {
    private final boolean listAllSubscription;
    private Subscription subscription;

    public AzureContainerRegistryComboBox(boolean listAllSubscription) {
        super(false);
        this.listAllSubscription = listAllSubscription;
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null && !listAllSubscription) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof ContainerRegistry) {
            final ContainerRegistry registry = (ContainerRegistry) item;
            return registry.isAdminUserEnabled() ? registry.getName() : String.format("%s (Admin User Disabled)", registry.getName());
        }
        return super.getItemText(item);
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object item) {
        return item instanceof ContainerRegistry ? IntelliJAzureIcons.getIcon(AzureIcons.ContainerRegistry.MODULE) : super.getItemIcon(item);
    }

    @Nonnull
    @Override
    protected List<? extends ContainerRegistry> loadItems() throws Exception {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return Collections.emptyList();
        }
        final Stream<Subscription> subscriptionStream = Optional.ofNullable(subscription).map(Stream::of).orElseGet(() -> !listAllSubscription ? Stream.empty() :
            Azure.az(AzureAccount.class).account().getSelectedSubscriptions().stream());
        return subscriptionStream.map(s -> Azure.az(AzureContainerRegistry.class).registry(s.getId()))
            .flatMap(module -> module.list().stream()).collect(Collectors.toList());
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
}
