/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.appinsight;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.ResourceBundle;

public class HDInsightCommonBundle {
    private static final ResourceBundle rb = ResourceBundle.getBundle("appinsight.messages");

    @NotNull
    public static String message(@NotNull String key) {
        return rb.getString(key);
    }
}
