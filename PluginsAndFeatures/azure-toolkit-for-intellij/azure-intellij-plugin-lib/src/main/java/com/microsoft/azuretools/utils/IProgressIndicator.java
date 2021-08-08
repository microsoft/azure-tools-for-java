/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

/**
 * Created by vlashch on 1/20/17.
 */
public interface IProgressIndicator {
    void setText(String text);
    void setText2(String text2);
    void setFraction(double fraction);
    boolean isCanceled();
    void notifyComplete();
}
