/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers.collections;

import java.util.Collection;
import java.util.EventObject;

public class ListChangedEvent extends EventObject {
    private ListChangedAction action;
    private Collection<?> newItems;
    private Collection<?> oldItems;

    public ListChangedEvent(
            ObservableList<?> source,
            ListChangedAction action,
            Collection<?> newItems,
            Collection<?> oldItems) {
        super(source);
        this.action = action;
        this.newItems = newItems;
        this.oldItems = oldItems;
    }

    public ListChangedAction getAction() {
        return action;
    }

    public Collection<?> getNewItems() {
        return newItems;
    }

    public Collection<?> getOldItems() {
        return oldItems;
    }
}
