package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.creation.ContainerAppsEnvironmentCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Stream;

public class AzureContainerAppsEnvironmentComboBox extends AzureComboBox<ContainerAppsEnvironment> {
    @Nullable
    private Subscription subscription;
    @Nullable
    private ResourceGroup resourceGroup;
    @Nullable
    private Region region;
    private final List<ContainerAppsEnvironment> draftItems = new LinkedList<>();

    @Override
    public String getLabel() {
        return "Container Apps Environment";
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof ContainerAppsEnvironment) {
            final ContainerAppsEnvironment environment = (ContainerAppsEnvironment) item;
            return (environment.isDraftForCreating() ? "(New) " + environment.getName() : environment.getName()) + "(" + environment.getRegion().getLabel() + ")";
        }
        return super.getItemText(item);
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

    public void setRegion(Region region) {
        if (Objects.equals(region, this.region)) {
            return;
        }
        this.region = region;
        if (region == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    public void setResourceGroup(ResourceGroup resourceGroup) {
        if (Objects.equals(resourceGroup, this.resourceGroup)) {
            return;
        }
        this.resourceGroup = resourceGroup;
        if (resourceGroup == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(@Nullable ContainerAppsEnvironment val, Boolean fixed) {
        if (Objects.nonNull(val) && val.isDraftForCreating() && !val.exists() && !this.draftItems.contains(val)) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nullable
    @Override
    protected ContainerAppsEnvironment doGetDefaultValue() {
        return CacheManager.getUsageHistory(ContainerAppsEnvironment.class)
            .peek(g -> Objects.isNull(subscription) || Objects.equals(subscription.getId(), g.getSubscriptionId()));
    }

    @Nonnull
    @Override
    protected List<? extends ContainerAppsEnvironment> loadItems() {
        Stream<ContainerAppsEnvironment> stream = Azure.az(AzureContainerApps.class).list().stream().flatMap(s -> s.environments().list().stream())
            .filter(env -> env.getFormalStatus().isConnected());
        Stream<ContainerAppsEnvironment> draftStream = this.draftItems.stream();
        if (Objects.nonNull(this.subscription)) {
            stream = stream.filter(env -> env.getSubscriptionId().equalsIgnoreCase(this.subscription.getId()));
            draftStream = draftStream.filter(env -> env.getSubscriptionId().equalsIgnoreCase(this.subscription.getId()));
        }
        if (Objects.nonNull(this.resourceGroup)) {
            stream = stream.filter(env -> Objects.isNull(env.getResourceGroup()) || Objects.equals(env.getResourceGroup(), this.resourceGroup));
            draftStream = draftStream.filter(env -> Objects.isNull(env.getResourceGroup()) || Objects.equals(env.getResourceGroup(), this.resourceGroup));
        }
        if (Objects.nonNull(this.region)) {
            stream = stream.filter(env -> Objects.equals(env.getRegion(), this.region));
            draftStream = draftStream.filter(env -> Objects.equals(env.getRegion(), this.region));
        }
        final List<ContainerAppsEnvironment> remoteEnvironments = stream
            .sorted(Comparator.comparing(ContainerAppsEnvironment::getName)).toList();
        final List<ContainerAppsEnvironment> environments = new ArrayList<>(remoteEnvironments);
        final ContainerAppsEnvironment draftItem = draftStream
            .filter(i -> !remoteEnvironments.contains(i)) // filter out the draft item which has been created
            .findFirst().orElse(null);
        if (Objects.nonNull(draftItem)) {
            if (CollectionUtils.isEmpty(environments) || (getValue() != null && getValue().isDraftForCreating())) {
                super.setValue(draftItem);
            }
            environments.add(draftItem);
        }
        return environments;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureContainerApps.class).containerApps(s.getId()).refresh());
        super.refreshItems();
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final List<ExtendableTextComponent.Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Create Environment (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.Add, tooltip, this::showEnvironmentCreationPopup);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void showEnvironmentCreationPopup() {
        final ContainerAppsEnvironmentCreationDialog dialog = new ContainerAppsEnvironmentCreationDialog(null);
        Optional.ofNullable(this.subscription).ifPresent(s -> dialog.setSubscription(s, true));
        Optional.ofNullable(this.resourceGroup).ifPresent(r -> dialog.setResourceGroup(r, true));
        Optional.ofNullable(this.region).ifPresent(r -> dialog.setRegion(r, true));
        final Action.Id<ContainerAppsEnvironmentDraft.Config> actionId = Action.Id.of("user/containerapps.create_container_apps_environment.environment");
        dialog.setOkAction(new Action<>(actionId)
            .withLabel("Create")
            .withIdParam(ContainerAppsEnvironmentDraft.Config::getName)
            .withSource(ContainerAppsEnvironmentDraft.Config::getResourceGroup)
            .withAuthRequired(true)
            .withHandler(config -> {
                final ContainerAppsEnvironmentDraft draft = Azure.az(AzureContainerApps.class).environments(config.getSubscription().getId())
                    .create(config.getName(), config.getResourceGroup().getName());
                draft.setConfig(config);
                this.setValue(draft);
            }));
        dialog.show();
    }
}
