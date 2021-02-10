/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class NodeAction implements Sortable, Groupable {
    private int priority = Sortable.DEFAULT_PRIORITY;
    private int group = Groupable.DEFAULT_GROUP;
    private String name;
    private boolean enabled = true;
    private List<NodeActionListener> listeners = new ArrayList<NodeActionListener>();
    private Node node; // the node with which this action is associated
    private String iconPath;
    @Getter
    @Setter
    private AzureIconSymbol iconSymbol;

    public NodeAction(Node node, String name) {
        this.node = node;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addListener(NodeActionListener listener) {
        listeners.add(listener);
    }

    public List<NodeActionListener> getListeners() {
        return listeners;
    }

    public void fireNodeActionEvent() {
        if (!listeners.isEmpty()) {
            final NodeActionEvent event = new NodeActionEvent(this);
            for (final NodeActionListener listener : listeners) {
                listener.beforeActionPerformed(event);
                Futures.addCallback(listener.actionPerformedAsync(event), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void voidParameter) {
                        listener.afterActionPerformed(event);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        listener.afterActionPerformed(event);
                    }
                }, MoreExecutors.directExecutor());
            }
        }
    }

    public Node getNode() {
        return node;
    }

    public boolean isEnabled() {
        // if the node to which this action is attached is in a
        // "loading" state then we disable the action regardless
        // of what "enabled" is
        return !node.isLoading() && enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }
}
