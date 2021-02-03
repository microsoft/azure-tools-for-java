/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.messagehandler;

import java.util.ResourceBundle;

public class PropUtil {
    static ResourceBundle rb = ResourceBundle.getBundle("com.microsoft.azuretools.azurecommons.messagehandler.messages");
    public static String getValueFromFile(String key) {
        String value = rb.getString(key);
        return value;
    }
}
