/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import lombok.Getter;
import lombok.Lombok;

class DelegateActionListener extends NodeActionListener {

    protected final NodeActionListener delegate;

    private DelegateActionListener(NodeActionListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) throws AzureCmdException {
        delegate.actionPerformed(e);
    }

    @Override
    public AzureIconSymbol getIconSymbol() {
        return delegate.getIconSymbol();
    }

    @Override
    public int getPriority() {
        return delegate.getPriority();
    }

    @Override
    public int getGroup() {
        return delegate.getGroup();
    }

    @Override
    protected String getServiceName(NodeActionEvent event) {
        return delegate.getServiceName(event);
    }

    @Override
    protected String getOperationName(NodeActionEvent event) {
        return delegate.getOperationName(event);
    }

    static final class BackgroundActionListener extends DelegateActionListener {

        private final String progressMessage;
        private boolean cancellable;
        private boolean conditionalModal;

        public BackgroundActionListener(NodeActionListener delegate, @NotNull String progressMessage, boolean cancellable, boolean conditionalModal) {
            super(delegate);
            this.progressMessage = progressMessage;
            this.cancellable = cancellable;
            this.conditionalModal = conditionalModal;
        }

        @Override
        public void actionPerformed(NodeActionEvent e) {
            Runnable runnable = () -> {
                try {
                    delegate.actionPerformed(e);
                } catch (AzureCmdException ex) {
                    Lombok.sneakyThrow(ex);
                }
            };
            final Object project = e.getAction().getNode().getProject();
            // todo: Add titles properties for services in common library
            final String key = String.format("%s.%s", BackgroundActionListener.super.getServiceName(e), BackgroundActionListener.super.getOperationName(e));
            AzureTask task = new AzureTask(project, AzureOperationBundle.title(key), cancellable, runnable);
            if (conditionalModal) {
                AzureTaskManager.getInstance().runInModal(task);
            } else {
                AzureTaskManager.getInstance().runInBackground(task);
            }
        }

    }

    static final class PromptActionListener extends DelegateActionListener {
        private static final String PROMPT_TITLE = "Azure Explorer";
        private static final String[] PROMPT_OPTIONS = new String[]{"Yes", "No"};

        private String promptMessage;

        public PromptActionListener(NodeActionListener delegate, @NotNull String promptMessage) {
            super(delegate);
            this.promptMessage = promptMessage;
        }

        @Override
        public void actionPerformed(NodeActionEvent e) throws AzureCmdException {
            boolean confirmed = DefaultLoader.getUIHelper().showConfirmation(promptMessage, PROMPT_TITLE, PROMPT_OPTIONS, null);
            if (confirmed) {
                delegate.actionPerformed(e);
            }
        }
    }

    static final class TelemetricActionListener extends DelegateActionListener {

        private String serviceName;
        private String operationName;

        public TelemetricActionListener(NodeActionListener delegate, String serviceName, String operationName) {
            super(delegate);
            this.serviceName = serviceName;
            this.operationName = operationName;
        }

        public TelemetricActionListener(NodeActionListener delegate, String actionString) {
            super(delegate);
            ActionConstants.ActionEntity action = ActionConstants.parse(actionString);
            this.serviceName = action.getServiceName();
            this.operationName = action.getOperationName();
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return serviceName;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return operationName;
        }
    }

    static final class BasicActionListener extends DelegateActionListener {

        @Getter
        private final AzureActionEnum actionEnum;

        public BasicActionListener(NodeActionListener delegate, AzureActionEnum actionEnum) {
            super(delegate);
            Preconditions.checkNotNull(actionEnum);
            this.actionEnum = actionEnum;
        }

        @Override
        public int getPriority() {
            return actionEnum.getPriority();
        }

        @Override
        public int getGroup() {
            return actionEnum.getGroup();
        }

        @Override
        public AzureIconSymbol getIconSymbol() {
            return actionEnum.getIconSymbol();
        }

        @Override
        public void actionPerformed(NodeActionEvent e) throws AzureCmdException {
            delegate.actionPerformed(e);
        }
    }

}
