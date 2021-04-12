/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs.livy;


import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;

public class LivySession {
    private int id;
    private String state;
    private String appId;
    private List<String> log;

    @NotNull
    public String getApplicationId() {
        return appId;
    }

    @NotNull
    public List<String> getLog() {
        return log;
    }

    public String getFormatLog() {
        StringBuilder stringBuilder = new StringBuilder();

        for(String str : log) {
            stringBuilder.append(str + "\n");
        }
        return stringBuilder.toString();
    }
}
