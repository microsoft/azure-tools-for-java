/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.servicebus.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.microsoft.azure.toolkit.intellij.common.component.SenderReceiverPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.servicebus.AzureServiceBusNamespace;
import com.microsoft.azure.toolkit.lib.servicebus.model.ServiceBusInstance;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class ServiceBusToolWindowManager {
    private static final String SERVICE_BUS_TOOL_WINDOW = "Azure Service Bus";
    private static final ServiceBusToolWindowManager instance = new ServiceBusToolWindowManager();
    private static final BidiMap<String, String> resourceIdToNameMap = new DualHashBidiMap<>();

    public static ServiceBusToolWindowManager getInstance() {
        return instance;
    }

    public void showServiceBusPanel(Project project, ServiceBusInstance<?, ?, ?> instance, boolean isListening) {
        final ToolWindow toolWindow =  ToolWindowManager.getInstance(project).getToolWindow(SERVICE_BUS_TOOL_WINDOW);
        if (Objects.isNull(toolWindow)) {
            return;
        }
        final String contentName = getConsoleViewName(instance.getId(), instance.getName());
        Content content = toolWindow.getContentManager().findContent(contentName);
        if (content == null) {
            final SenderReceiverPanel panel = new SenderReceiverPanel(project, instance);
            content = ContentFactory.SERVICE.getInstance().createContent(panel, contentName, false);
            toolWindow.getContentManager().addContent(content);
        }
        final JComponent contentComponent = content.getComponent();
        if (contentComponent instanceof SenderReceiverPanel && isListening) {
            ((SenderReceiverPanel) contentComponent).startListeningProcess();
        }
        toolWindow.getContentManager().setSelectedContent(content);
        toolWindow.setAvailable(true);
        toolWindow.activate(null);
    }

    public void stopListening(Project project, ServiceBusInstance<?, ?, ?> instance) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(SERVICE_BUS_TOOL_WINDOW);
        final String contentName = getConsoleViewName(instance.getId(), instance.getName());
        final Content content = Optional.ofNullable(toolWindow).map(w -> w.getContentManager().findContent(contentName)).orElse(null);
        if (Objects.isNull(content)) {
            return;
        }
        final JComponent contentComponent = content.getComponent();
        if (contentComponent instanceof SenderReceiverPanel) {
            ((SenderReceiverPanel) contentComponent).stopListeningProcess();
        }
    }

    private String getConsoleViewName(String resourceId, String resourceName) {
        if (resourceIdToNameMap.containsKey(resourceId)) {
            return resourceIdToNameMap.get(resourceId);
        }
        String result = resourceName;
        int i = 1;
        while (resourceIdToNameMap.containsValue(result)) {
            result = String.format("%s(%s)", resourceName, i++);
        }
        resourceIdToNameMap.put(resourceId, result);
        return result;
    }

    public static class ServiceBusToolWindowFactory implements ToolWindowFactory {
        @Override
        public void init(@NotNull ToolWindow toolWindow) {
            toolWindow.setToHideOnEmptyContent(true);
        }

        @Override
        public boolean shouldBeAvailable(@NotNull Project project) {
            return false;
        }

        @Override
        public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
            toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
                @Override
                public void contentRemoved(@NotNull ContentManagerEvent event) {
                    final String displayName = event.getContent().getDisplayName();
                    final String removeResourceId = resourceIdToNameMap.getKey(displayName);
                    Optional.ofNullable(removeResourceId).ifPresent(r -> {
                        final ServiceBusInstance<?,?,?> instance = Azure.az(AzureServiceBusNamespace.class).getById(r);
                        Optional.ofNullable(instance).ifPresent(ServiceBusInstance::stopReceivingMessage);
                        resourceIdToNameMap.removeValue(displayName);
                        final JComponent contentComponent = event.getContent().getComponent();
                        if (contentComponent instanceof SenderReceiverPanel) {
                            ((SenderReceiverPanel) contentComponent).dispose();
                        }
                    });
                }

                @Override
                public void contentRemoveQuery(@NotNull ContentManagerEvent event) {
                    final String displayName = event.getContent().getDisplayName();
                    final String removeResourceId = resourceIdToNameMap.getKey(displayName);
                    Optional.ofNullable(removeResourceId).ifPresent(r -> {
                        final ServiceBusInstance<?,?,?> instance = Azure.az(AzureServiceBusNamespace.class).getById(r);
                        Optional.ofNullable(instance).ifPresent(s -> {
                            if (s.isListening()) {
                                final boolean canClose = AzureMessager.getMessager().confirm(AzureString.format(
                                        "This will stop listening to Service Bus \"{0}\", are you sure to do this?", displayName));
                                if (!canClose) {
                                    event.consume();
                                }
                            }
                        });
                    });
                }
            });
        }
    }
}
