/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.report;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by vlashch on 10/20/16.
 */
public class Reporter<T> {

    private Set<IListener<T>> listeners = new HashSet<>();

    public IListener<T> addListener(IListener<T> listener) {
        listeners.add(listener);
        return listener;
    }

    public void report(T message) {
        for (IListener<T> l : listeners) {
            l.listen(message);
        }
    }

    public void addConsoleLister() {
        addListener(new ConsoleListener<T>());
    }
}
