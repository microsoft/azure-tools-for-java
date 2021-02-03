/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.interact;

/**
 * Created by shch on 10/12/2016.
 */
public interface INotification {
    void deliver(String subject, String message);
}
