/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import java.util.EventObject;

public class NodeActionEvent extends EventObject {
    public NodeActionEvent(NodeAction action) {
        super(action);
    }

    public NodeAction getAction() {
        return (NodeAction) getSource();
    }
}
