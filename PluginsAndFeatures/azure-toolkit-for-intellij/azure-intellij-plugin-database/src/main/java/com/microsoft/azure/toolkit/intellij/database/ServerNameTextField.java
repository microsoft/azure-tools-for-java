/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database;

import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Getter;

import java.util.Objects;

public class ServerNameTextField extends AzureTextInput {
    @Getter
    private Subscription subscription;

    public ServerNameTextField() {
        super();
        this.setRequired(true);
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        this.validateValueAsync();
    }
}
