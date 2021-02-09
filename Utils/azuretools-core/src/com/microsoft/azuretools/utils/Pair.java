/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

/**
 * Created by vlashch on 10/6/16.
 */
public class Pair<F, S> {
    private F f;
    private S s;
    public Pair(F f,S s) {
        this.f = f;
        this.s = s;
    }

    public F first() {
        return f;
    }

    public S second() {
        return s;
    }
}
