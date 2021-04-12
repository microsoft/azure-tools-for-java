/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.report;

/**
 * Created by vlashch on 10/20/16.
 */
public class ConsoleListener<T> implements IListener<T> {
    @Override
    public void listen(T message) {
        System.out.println(message);
    }
}
