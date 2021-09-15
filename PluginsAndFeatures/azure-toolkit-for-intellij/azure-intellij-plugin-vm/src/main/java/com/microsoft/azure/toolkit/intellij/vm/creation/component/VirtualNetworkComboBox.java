package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import com.microsoft.azure.toolkit.lib.compute.network.AzureNetwork;
import com.microsoft.azure.toolkit.lib.compute.network.DraftNetwork;
import com.microsoft.azure.toolkit.lib.compute.network.Network;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class VirtualNetworkComboBox extends AzureComboBox<Network> {
    private DraftNetwork draftNetwork;
    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private Region region;

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        Optional.ofNullable(draftNetwork).ifPresent(draftNetwork -> draftNetwork.setSubscriptionId(subscription.getId()));
        this.refreshItems();
    }

    public void setResourceGroup(ResourceGroup resourceGroup) {
        this.resourceGroup = resourceGroup;
        Optional.ofNullable(draftNetwork).ifPresent(draftNetwork -> draftNetwork.setResourceGroup(resourceGroup.getName()));
    }

    public void setRegion(Region region) {
        this.region = region;
        Optional.ofNullable(draftNetwork).ifPresent(draftNetwork -> draftNetwork.setRegion(region));
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(AllIcons.General.Add, "Create new virtual network", this::createVirtualNetwork);
    }

    private void createVirtualNetwork() {
        if (!ObjectUtils.allNotNull(resourceGroup, region, subscription)) {
            AzureMessager.getMessager().warning("To create new virtual network, please select subscription, resource group and region first");
            return;
        }
        final DraftNetwork defaultNetwork = new DraftNetwork();
        defaultNetwork.setName("network");
        defaultNetwork.setAddressSpace("10.0.2.0/24");
        defaultNetwork.setSubnet("default");
        defaultNetwork.setSubnetAddressSpace("10.0.2.0/24");
        final VirtualNetworkDialog dialog = new VirtualNetworkDialog(subscription.getId(), resourceGroup.getName(), region);
        dialog.setData(defaultNetwork);
        if (dialog.showAndGet()) {
            this.draftNetwork = dialog.getData();
            this.addItem(draftNetwork);
            setValue(draftNetwork);
        }
    }

    @Override
    public void setValue(Network value) {
        if (value instanceof DraftNetwork && value.status() == IAzureBaseResource.Status.DRAFT) {
            draftNetwork = (DraftNetwork) value;
        }
        super.setValue(value);
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Network) {
            return item instanceof AzureResourceDraft ? "(New) " + ((Network) item).name() : ((Network) item).name();
        }
        return super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends Network> loadItems() throws Exception {
        final List<Network> networks = Optional.ofNullable(subscription).map(subscription -> Azure.az(AzureNetwork.class).list(subscription.getId())).orElse(Collections.emptyList());
        return draftNetwork == null ? networks : ListUtils.union(List.of(draftNetwork), networks);
    }
}
