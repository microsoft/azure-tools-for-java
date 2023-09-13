/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.eventhubs.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.*;
import com.microsoft.azure.toolkit.intellij.common.component.SenderReceiverPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.eventhubs.AzureEventHubsNamespace;
import com.microsoft.azure.toolkit.lib.eventhubs.EventHubsInstance;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class EventHubsToolWindowManager {
    private static final String EVENT_HUBS_TOOL_WINDOW = "Azure Event Hubs";
    private static final EventHubsToolWindowManager instance = new EventHubsToolWindowManager();
    private static final BidiMap<String, String> resourceIdToNameMap = new DualHashBidiMap<>();

    public static  EventHubsToolWindowManager getInstance() {
        return instance;
    }

    public void showEventHubsPanel(Project project, EventHubsInstance instance, boolean isListening) {
        final ToolWindow toolWindow =  ToolWindowManager.getInstance(project).getToolWindow(EVENT_HUBS_TOOL_WINDOW);
        final String contentName = getConsoleViewName(instance.getId(), instance.getName());
        Content content = Optional.ofNullable(toolWindow).map(w -> w.getContentManager().findContent(contentName)).orElse(null);
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

    public void stopListening(Project project, EventHubsInstance instance) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(EVENT_HUBS_TOOL_WINDOW);
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

    public static class EventHubsToolWindowFactory implements ToolWindowFactory {
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
                        final EventHubsInstance instance = Azure.az(AzureEventHubsNamespace.class).getById(r);
                        Optional.ofNullable(instance).ifPresent(EventHubsInstance::stopReceivingMessage);
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
                        final EventHubsInstance instance = Azure.az(AzureEventHubsNamespace.class).getById(r);
                        Optional.ofNullable(instance).ifPresent(e -> {
                            if (e.isListening()) {
                                final boolean canClose = AzureMessager.getMessager().confirm(AzureString.format(
                                        "This will stop listening to Event Hubs \"{0}\", are you sure to do this?", displayName));
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
