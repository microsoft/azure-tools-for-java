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

package com.microsoft.tooling.msservices.serviceexplorer;

import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.*;

public class DefaultAzureResourceTracker {
    private Map<String, List<IDataRefreshableComponent>> idToNodes = new HashMap<>();

    public static DefaultAzureResourceTracker getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void registerNode(String id, IDataRefreshableComponent node) {
        synchronized (idToNodes) {
            idToNodes.compute(id, (k, v) -> {
                if (v == null) {
                    return new ArrayList<>(Arrays.asList(node));
                } else {
                    v.add(node);
                }
                return v;
            });
        }
    }

    public void unregisterNode(String id, IDataRefreshableComponent node) {
        synchronized (idToNodes) {
            idToNodes.computeIfPresent(id, (k, v) -> {
                if (v == null) {
                    return v;
                } else {
                    v.remove(node);
                }
                return v;
            });
        }
    }

    public <T, U> void handleDataChanges(String id, T data1, U data2) {
        synchronized (idToNodes) {
            List<IDataRefreshableComponent> list = idToNodes.get(id);
            if (list != null && !list.isEmpty()) {
                final List<IDataRefreshableComponent> toNotify = new ArrayList<>(list);
                DefaultLoader.getIdeHelper().invokeLater(() -> {
                    for (IDataRefreshableComponent<T, U> node : toNotify) {
                        node.notifyDataRefresh(data1, data2);
                    }
                });

            }
        }
    }

    private static final class SingletonHolder {
        private static final DefaultAzureResourceTracker INSTANCE = new DefaultAzureResourceTracker();

        private SingletonHolder() {
        }
    }
}
