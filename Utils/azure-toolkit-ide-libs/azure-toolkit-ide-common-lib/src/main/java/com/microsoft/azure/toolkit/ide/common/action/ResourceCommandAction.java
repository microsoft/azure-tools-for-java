/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class ResourceCommandAction<T extends AzResource> extends Action<T> {

    private Function<T, String> commandProvider;

    public ResourceCommandAction(@Nonnull Id<T> id) {
        super(id);
        this.withHandler((d, e) -> {
            final String command = this.commandProvider.apply(d);
            if (StringUtils.isNoneBlank(command)) {
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.INVOKE_COMMAND_IN_TERMINAL).handle(command, e);
            }
        });
    }

    public ResourceCommandAction<T> withCommand(@Nonnull final String command) {
        this.commandProvider = (any) -> command;
        return this;
    }

    public ResourceCommandAction<T> withCommand(@Nonnull final Function<T, String> commandProvider) {
        this.commandProvider = commandProvider;
        return this;
    }
}
