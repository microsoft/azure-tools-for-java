/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.intellij.common.settings.AzureConfigurations;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Setter
@Slf4j
public class IntellijNeverShowAgainAction extends NotificationAction {

    public IntellijNeverShowAgainAction() {
        super("Never Show Again");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event, @NotNull Notification notification) {
        Optional.ofNullable(ActionManager.getInstance().getId(this)).ifPresent(id -> {
            AzureConfigurations.getInstance().getState().getSuppressedActions().put(id, Boolean.TRUE);
            notification.expire();
        });
    }

}
