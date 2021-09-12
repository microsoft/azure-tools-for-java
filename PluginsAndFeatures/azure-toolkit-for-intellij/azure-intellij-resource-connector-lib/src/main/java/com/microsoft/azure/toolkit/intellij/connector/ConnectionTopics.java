/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.util.messages.Topic;
import com.microsoft.azure.toolkit.intellij.connector.lib.Connection;

public interface ConnectionTopics {

    Topic<ConnectionChanged> CONNECTION_CHANGED = Topic.create("connector.connection.changed", ConnectionChanged.class);
    Topic<ConnectionsRefreshed> CONNECTIONS_REFRESHED = Topic.create("connector.connections.refreshed", ConnectionsRefreshed.class);

    interface ConnectionChanged {
        void connectionChanged(Connection<?, ?> connection);
    }

    interface ConnectionsRefreshed {
        void connectionsRefreshed();
    }
}
