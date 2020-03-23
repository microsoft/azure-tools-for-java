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

package com.microsoft.tooling.msservices.helpers.collections;

import com.google.common.collect.ForwardingList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ObservableList<E> extends ForwardingList<E> {
    private final List<E> delegate = new ArrayList<E>();
    protected List<ListChangeListener> changeListeners = new ArrayList<ListChangeListener>();
    private boolean freezeEvents = false;

    @Override
    protected List<E> delegate() {
        return delegate;
    }

    @Override
    public boolean add(E element) {
        return standardAdd(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        freezeEvents = true;
        boolean changed = standardAddAll(c);
        if (changed) {
            freezeEvents = false;
            fireChangeListenerEvent(ListChangedAction.add, c, null);
        }

        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        freezeEvents = true;
        boolean changed = standardAddAll(index, c);
        if (changed) {
            freezeEvents = false;
            fireChangeListenerEvent(ListChangedAction.add, c, null);
        }

        return changed;
    }

    @Override
    public void add(int index, E element) {
        super.add(index, element);

        // notify post-add
        fireChangeListenerEvent(
                ListChangedAction.add,
                Arrays.asList(element),
                null);
    }

    @Override
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index > -1)
            remove(index);
        return index != -1;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        freezeEvents = true;
        boolean changed = standardRemoveAll(c);
        if (changed) {
            freezeEvents = false;
            fireChangeListenerEvent(ListChangedAction.remove, null, c);
        }

        return standardRemoveAll(c);
    }

    @Override
    public E remove(int index) {
        E removed = super.remove(index);

        // notify post-remove
        fireChangeListenerEvent(
                ListChangedAction.remove,
                null,
                Arrays.asList(removed));

        return removed;
    }

    private void fireChangeListenerEvent(ListChangedAction action,
                                         Collection<?> newItems,
                                         Collection<?> oldItems) {
        if (!changeListeners.isEmpty() && !freezeEvents) {
            ListChangedEvent listChangedEvent = new ListChangedEvent(
                    this,
                    action,
                    newItems,
                    oldItems);
            for (ListChangeListener listener : changeListeners) {
                listener.listChanged(listChangedEvent);
            }
        }
    }

    public void addChangeListener(ListChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(ListChangeListener listener) {
        changeListeners.remove(listener);
    }

    public void removeAllChangeListeners() {
        // we remove items from the collection one by one instead
        // of simply calling "clear" because we want the "remove" event
        // to fire for each item that's removed
        while (!changeListeners.isEmpty()) {
            changeListeners.remove(0);
        }
    }
}
