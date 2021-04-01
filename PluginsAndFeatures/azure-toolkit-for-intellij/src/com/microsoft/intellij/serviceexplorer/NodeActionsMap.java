/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import java.util.Map;

public abstract class NodeActionsMap {
    public static final ExtensionPointName<NodeActionsMap> EXTENSION_POINT_NAME =
            ExtensionPointName.create("com.microsoft.intellij.nodeActionsMap");

    public abstract Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> getMap();
}
