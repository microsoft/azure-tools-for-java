/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common;

import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Getter;
import lombok.Setter;

public class DraftResourceGroup extends ResourceGroup implements Draft {
    @Getter
    @Setter
    private Subscription subscription;

    public DraftResourceGroup(Subscription s, String name) {
        super(builder().subscriptionId(s.getId()).name(name));
        this.subscription = s;
    }
}
