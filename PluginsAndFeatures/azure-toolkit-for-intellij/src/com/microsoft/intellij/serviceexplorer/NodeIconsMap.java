/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.serviceexplorer;

import com.google.common.collect.ImmutableMap;
import com.microsoft.intellij.helpers.AzureAllIcons;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeState;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class NodeIconsMap {

    public static final Map<AzureActionEnum, Icon> BASIC_ACTION_TO_ICON_MAP = new HashMap<>();
    public static final Map<Class<? extends Node>, Icon> NODE_TO_ICON_MAP = new HashMap<>();
    public static final Map<Class<? extends Node>, ImmutableMap<NodeState, Icon>> NODE_TO_ICON_WITH_STATE_MAP = new HashMap<>();

    static {
        // basic action to icon map.
        BASIC_ACTION_TO_ICON_MAP.put(AzureActionEnum.REFRESH, AzureAllIcons.Common.RESTART);
        BASIC_ACTION_TO_ICON_MAP.put(AzureActionEnum.CREATE, AzureAllIcons.Common.CREATE);
        BASIC_ACTION_TO_ICON_MAP.put(AzureActionEnum.START, AzureAllIcons.Common.START);
        BASIC_ACTION_TO_ICON_MAP.put(AzureActionEnum.STOP, AzureAllIcons.Common.STOP);
        BASIC_ACTION_TO_ICON_MAP.put(AzureActionEnum.RESTART, AzureAllIcons.Common.RESTART);
        BASIC_ACTION_TO_ICON_MAP.put(AzureActionEnum.DELETE, AzureAllIcons.Common.DELETE);
        BASIC_ACTION_TO_ICON_MAP.put(AzureActionEnum.OPEN_IN_PORTAL, AzureAllIcons.Common.OPEN_IN_PORTAL);
        BASIC_ACTION_TO_ICON_MAP.put(AzureActionEnum.SHOW_PROPERTIES, AzureAllIcons.Common.SHOW_PROPERTIES);

        // node to icon map.
        NODE_TO_ICON_MAP.put(MySQLModule.class, AzureAllIcons.MySQL.MODULE);

        // node to icon with state map.
        NODE_TO_ICON_WITH_STATE_MAP.put(MySQLNode.class, new ImmutableMap.Builder<NodeState, Icon>()
                .put(NodeState.RUNNING, AzureAllIcons.MySQL.RUNNING)
                .put(NodeState.STOPPED, AzureAllIcons.MySQL.STOPPED)
                .put(NodeState.UPDATING, AzureAllIcons.MySQL.UPDATING)
                .build());
    }
}
