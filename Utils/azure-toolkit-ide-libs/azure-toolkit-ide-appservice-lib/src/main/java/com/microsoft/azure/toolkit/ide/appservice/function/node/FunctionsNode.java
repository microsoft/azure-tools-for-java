/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function.node;

import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.stream.Collectors;

public class FunctionsNode extends Node<IFunctionApp> {
    private final IFunctionApp functionApp;
    private final NodeView nodeView;

    public FunctionsNode(@Nonnull IFunctionApp functionApp) {
        super(functionApp);
        this.functionApp = functionApp;
        this.nodeView = new FunctionsNodeView(functionApp);
        this.actions(FunctionAppActionsContributor.FUNCTIONS_ACTIONS);
        this.addChildren(ignore -> functionApp.listFunctions().stream()
                        .sorted(Comparator.comparing(FunctionEntity::getName)).collect(Collectors.toList()),
                (function, functionsNode) -> new Node<>(function)
                        .view(new NodeView.Static(function.getName(), "/icons/function-trigger.png"))
                        .actions(FunctionAppActionsContributor.FUNCTION_ACTION));
    }

    @Nonnull
    @Override
    public NodeView view() {
        return this.nodeView;
    }

    static class FunctionsNodeView implements NodeView {

        @Nonnull
        @Getter
        private final IFunctionApp functionApp;
        private final AzureEventBus.EventListener<Object, AzureEvent<Object>> listener;

        @Nullable
        @Setter
        @Getter
        private Refresher refresher;

        public FunctionsNodeView(@Nonnull IFunctionApp functionApp) {
            this.functionApp = functionApp;
            this.listener = new AzureEventBus.EventListener<>(this::onEvent);
            AzureEventBus.on("appservice|function.functions.refresh", listener);
            this.refreshView();
        }

        @Override
        public String getLabel() {
            return "Functions";
        }

        @Override
        public String getIconPath() {
            return "/icons/functionapp.png";
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

        public void onEvent(AzureEvent<Object> event) {
            final Object source = event.getSource();
            if (source instanceof IAzureBaseResource && ((IAzureBaseResource<?, ?>) source).id().equals(this.functionApp.id())) {
                AzureTaskManager.getInstance().runLater(this::refreshChildren);
            }
        }
    }
}
