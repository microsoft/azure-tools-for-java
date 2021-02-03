/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class BasicActionBuilder {

    private static final String FUZZY_PROGRESS_MESSAGE_PATTERN = "%s...";
    private static final String GENERIC_PROGRESS_MESSAGE_PATTERN = "%s %s...";
    private static final String FULL_PROGRESS_MESSAGE_PATTERN = "%s %s (%s)...";

    private static final String PROMPT_MESSAGE_PATTERN = "This operation will %s your %s: %s. Are you sure you want to continue?";

    private Runnable runnable;
    private AzureActionEnum action;

    private String moduleName;
    private String instanceName;

    private boolean backgroundable;
    private boolean backgroundCancellable;
    private boolean backgroundConditionalModal;

    private boolean promptable;

    public BasicActionBuilder(Runnable runnable) {
        Preconditions.checkNotNull(runnable);
        this.runnable = runnable;
    }

    public BasicActionBuilder withAction(final AzureActionEnum action) {
        this.action = action;
        return this;
    }

    public BasicActionBuilder withModuleName(final String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    public BasicActionBuilder withInstanceName(final String instanceName) {
        this.instanceName = instanceName;
        return this;
    }

    public BasicActionBuilder withBackgroudable(final boolean backgroundRequired) {
        this.backgroundable = backgroundRequired;
        return this;
    }

    public BasicActionBuilder withBackgroudable(final boolean backgroundRequired, final boolean cancellable, final boolean conditionalModal) {
        this.backgroundable = backgroundRequired;
        this.backgroundCancellable = cancellable;
        this.backgroundConditionalModal = conditionalModal;
        return this;
    }

    public BasicActionBuilder withPromptable(final boolean promptRequired) {
        this.promptable = promptRequired;
        return this;
    }

    public DelegateActionListener.BasicActionListener build() {
        Preconditions.checkNotNull(Objects.nonNull(action));
        NodeActionListener delegate = this.innerBuild(null, null);
        return new DelegateActionListener.BasicActionListener(delegate, action);
    }

    public NodeActionListener build(final String doingName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(doingName));
        Preconditions.checkArgument(!promptable);
        return this.innerBuild(doingName, null);
    }

    private NodeActionListener innerBuild(final String doingName, final String actionName) {
        Preconditions.checkNotNull(runnable);
        NodeActionListener delegate = new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                runnable.run();
            }
        };
        if (backgroundable) {
            delegate = new DelegateActionListener.BackgroundActionListener(delegate,
                    getProgressMessage(doingName), backgroundCancellable, backgroundConditionalModal);
        }
        if (promptable) {
            delegate = new DelegateActionListener.PromptActionListener(delegate, getPromptMessage(actionName));
        }
        return delegate;
    }

    private String getProgressMessage(final String doingName) {
        Preconditions.checkArgument(Objects.nonNull(action) || StringUtils.isNotBlank(doingName));
        String realDoingName = StringUtils.firstNonBlank(doingName, action.getDoingName());
        if (StringUtils.isNotBlank(moduleName) && StringUtils.isNotBlank(instanceName)) {
            return String.format(FULL_PROGRESS_MESSAGE_PATTERN, realDoingName, moduleName, instanceName);
        }
        if (StringUtils.isNotBlank(moduleName)) {
            return String.format(GENERIC_PROGRESS_MESSAGE_PATTERN, realDoingName, moduleName);
        }
        if (StringUtils.isNotBlank(instanceName)) {
            return String.format(GENERIC_PROGRESS_MESSAGE_PATTERN, realDoingName, instanceName);
        }
        return String.format(FUZZY_PROGRESS_MESSAGE_PATTERN, realDoingName);
    }

    private String getPromptMessage(final String actionName) {
        String realActionName = StringUtils.firstNonBlank(actionName, action.getName().toLowerCase());
        return String.format(PROMPT_MESSAGE_PATTERN, realActionName, moduleName, instanceName);
    }

}
