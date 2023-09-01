/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function.node;

import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionsNode extends Node<FunctionApp> {
    private final FunctionApp functionApp;

    public FunctionsNode(@Nonnull FunctionApp functionApp) {
        super(functionApp);
        this.functionApp = functionApp;
        this.view(new FunctionsNodeView(functionApp));
        this.actions(FunctionAppActionsContributor.FUNCTIONS_ACTIONS);
        this.addChildren(this::listChildren, (function, functionsNode) -> new Node<>(function)
                .view(new NodeView.Static(function.getName(), "/icons/function-trigger.png", getFunctionTriggerType(function)))
                .doubleClickAction(FunctionAppActionsContributor.TRIGGER_FUNCTION)
                .actions(FunctionAppActionsContributor.FUNCTION_ACTION));
    }

    private List<FunctionEntity> listChildren(FunctionApp functionApp) {
        try {
            return functionApp.listFunctions().stream().sorted(Comparator.comparing(FunctionEntity::getName)).collect(Collectors.toList());
        } catch (final Exception e) {
            AzureMessager.getMessager().error(String.format("failed to list triggers in function app %s : %s", functionApp.getName(), e.getMessage()));
            return Collections.emptyList();
        }
    }

    private static String getFunctionTriggerType(@Nonnull FunctionEntity functionEntity) {
        return Optional.ofNullable(functionEntity.getTrigger()).map(FunctionEntity.BindingEntity::getType).map(StringUtils::capitalize).orElse("Unknown");
    }

    static class FunctionsNodeView implements NodeView {

        @Nonnull
        @Getter
        private final FunctionApp functionApp;
        private final AzureEventBus.EventListener listener;

        @Nullable
        @Setter
        @Getter
        private Refresher refresher;

        public FunctionsNodeView(@Nonnull FunctionApp functionApp) {
            this.functionApp = functionApp;
            this.listener = new AzureEventBus.EventListener(this::onEvent);
            AzureEventBus.on("appservice|function.functions.refresh", listener);
            this.refreshView();
        }

        @Override
        public String getLabel() {
            return "Functions";
        }

        @Override
        public String getIconPath() {
            return AzureIcons.FunctionApp.MODULE.getIconPath();
        }

        @Override
        public String getDescription() {
            return getLabel();
        }

        @Override
        public void dispose() {
            AzureEventBus.off("appservice|function.functions.refresh", listener);
            this.refresher = null;
        }

        public void onEvent(AzureEvent event) {
            final Object source = event.getSource();
            if (source instanceof AzResource && ((AzResource<?, ?, ?>) source).id().equals(this.functionApp.id())) {
                AzureTaskManager.getInstance().runLater(this::refreshChildren);
            }
        }
    }
}
