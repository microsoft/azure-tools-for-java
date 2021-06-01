/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

@RequiredArgsConstructor
@Setter
@Slf4j
public class IntellijActionMessageAction implements IAzureMessage.Action {
    @Getter
    @Nonnull
    private final String actionId;

    @Override
    public String name() {
        return actionId;
    }

    @Override
    public void actionPerformed(IAzureMessage payload) {
        log.warn("action\"{}\" not found", actionId);
    }
}
