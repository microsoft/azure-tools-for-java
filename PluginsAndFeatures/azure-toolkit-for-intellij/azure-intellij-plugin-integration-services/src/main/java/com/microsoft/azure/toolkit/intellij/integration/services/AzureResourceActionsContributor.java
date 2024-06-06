package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.execution.services.ServiceEventListener;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.intellij.integration.services.connectionstring.AzureResourceConnectDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Objects;

public class AzureResourceActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    public static final Action.Id<Object> ADD_RESOURCE = Action.Id.of("user/servicesview.add_resource");
    public static final Action.Id<Object> CONNECT_RESOURCE = Action.Id.of("user/servicesview.connect_resource");
    public static final Action.Id<Azure> REFRESH = Action.Id.of("user/servicesview.refresh_all");
    public static final Action.Id<AbstractAzResource<?, ?, ?>> REMOVE_RESOURCE = Action.Id.of("user/servicesview.remove_resource");
    public static final String CONNECTION_STRING_RESOURCES = "Connection String Resources";

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(ADD_RESOURCE)
            .withIcon(AzureIcons.Action.ADD.getIconPath())
            .withLabel("Select resources under signed in account...")
            .withHandler((s, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final AnActionEvent event = (AnActionEvent) e;
                final AzureResourceSelectDialog dialog = new AzureResourceSelectDialog(event.getProject());
                dialog.setOkActionListener((resources) -> {
                    dialog.close();
                    if (Objects.nonNull(resources)) {
                        final AzureResourceManager manager = AzureResourceManager.getInstance();
                        manager.addResources(resources);
                    }
                });
                dialog.show();
            }))
            .withAuthRequired(true)
            .register(am);
        new Action<>(CONNECT_RESOURCE)
            .withIcon(AzureIcons.Action.ADD.getIconPath())
            .withLabel("Connect resource using connection string...")
            .withHandler((s, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final AnActionEvent event = (AnActionEvent) e;
                final AzureResourceConnectDialog dialog = new AzureResourceConnectDialog(event.getProject());
                dialog.setOkActionListener((resource) -> {
                    dialog.close();
                    if (Objects.nonNull(resource)) {
                        AzureStoreManager.getInstance().getSecureStore().savePassword(CONNECTION_STRING_RESOURCES, resource.getId().toLowerCase(), null, resource.getConnectionString());
                        AzureResourceManager.getInstance().addResource(resource);
                    }
                });
                dialog.show();
            }))
            .withAuthRequired(false)
            .register(am);
        new Action<>(REFRESH)
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withLabel("Refresh")
            .withHandler((s, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
                bus.syncPublisher(ServiceEventListener.TOPIC).handle(ServiceEventListener.ServiceEvent.createResetEvent(AzureServiceViewContributor.class));
            }))
            .withAuthRequired(false)
            .register(am);
        new Action<>(REMOVE_RESOURCE)
            .withIcon(AzureIcons.Action.REMOVE.getIconPath())
            .withLabel("Remove Resource")
            .withHandler(s -> AzureResourceManager.getInstance().removeResource(s))
            .withAuthRequired(false)
            .register(am);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
