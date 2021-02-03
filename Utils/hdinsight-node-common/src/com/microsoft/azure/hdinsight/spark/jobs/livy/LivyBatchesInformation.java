/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs.livy;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.List;

public class LivyBatchesInformation {
    private int from;
    private int total;
    private List<LivySession> sessions;

    @Nullable
    public LivySession getSession(@NotNull String applicationId) {
        for(LivySession session : sessions) {
            if(session.getApplicationId().equals(applicationId)) {
                return session;
            }
        }
        return null;
    }
}
