/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common;

import com.microsoft.azure.toolkit.ide.common.component.Node;

public interface IExplorerContributor {
    Node<?> getNode();

    default int getOrder() {
        return 0;
    }
}
