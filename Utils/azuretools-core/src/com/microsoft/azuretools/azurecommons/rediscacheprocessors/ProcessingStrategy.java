/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

public interface ProcessingStrategy {
    ProcessingStrategy process() throws InterruptedException;

    void waitForCompletion(String produce) throws InterruptedException;

    void notifyCompletion() throws InterruptedException;
}
