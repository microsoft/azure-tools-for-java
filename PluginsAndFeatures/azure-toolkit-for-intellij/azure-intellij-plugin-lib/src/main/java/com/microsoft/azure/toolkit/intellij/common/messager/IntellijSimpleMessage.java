/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.messager.SimpleMessage;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;

@Getter
@Setter
public class IntellijSimpleMessage extends SimpleMessage {
    private Project project;
    private Boolean backgrounded;

    public IntellijSimpleMessage(@Nonnull Type type, @Nonnull String message) {
        super(type, message);
    }
}
