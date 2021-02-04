/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
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
            AzureTask task = new AzureTask(project, progressMessage, cancellable, runnable);
            if (conditionalModal) {
                AzureTaskManager.getInstance().runInModal(task);
            } else {
                AzureTaskManager.getInstance().runInBackground(task);
            }
        }

    }

    static final class PromptActionListener extends DelegateActionListener {
        private static final String PROMPT_TITLE = "Azure Explorer";
        private static final String[] PROMPT_OPTIONS = new String[] {"Yes", "No"};

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
        public void actionPerformed(NodeActionEvent e) throws AzureCmdException {
            sendTelemetry(e);
            Operation operation = TelemetryManager.createOperation(serviceName, operationName);
            try {
                operation.start();
                Node node = e.getAction().getNode();
                EventUtil.logEvent(EventType.info, operation, buildProp(node));
                delegate.actionPerformed(e);
            } finally {
                operation.complete();
            }
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
