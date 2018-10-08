package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;
import java.util.List;

public class DeploymentSlotModule extends AzureRefreshableNode implements DeploymentSlotModuleView {
    private static final String MODULE_ID = WebAppModule.class.getName();
    private static final String ICON_PATH = "Slot_16.png";
    private static final String MODULE_NAME = "Deployment Slots";

    private final DeploymentSlotModulePresenter presenter;
    protected final String subscriptionId;
    protected final String webAppId;

    public DeploymentSlotModule(final Node parent, final String subscriptionId, final String webAppId) {
        super(MODULE_ID, MODULE_NAME, parent, ICON_PATH);
        this.subscriptionId = subscriptionId;
        this.webAppId = webAppId;
        presenter = new DeploymentSlotModulePresenter<>();
        presenter.onAttachView(this);
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        try {
            presenter.onRefreshDeploymentSlotModule(this.subscriptionId, this.webAppId);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void renderDeploymentSlots(@NotNull final List<DeploymentSlot> slots) {
        slots.forEach(slot -> addChildNode(new DeploymentSlotNode(
            this, slot.name(), slot.state(), slot.operatingSystem().toString(), this.subscriptionId)));
    }
}
