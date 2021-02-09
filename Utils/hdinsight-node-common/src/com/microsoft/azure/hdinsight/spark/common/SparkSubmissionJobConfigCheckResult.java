/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class SparkSubmissionJobConfigCheckResult implements Comparable<SparkSubmissionJobConfigCheckResult> {

    private SparkSubmissionJobConfigCheckStatus status;
    private String messaqge;

    public SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus status, String message){
        this.status = status;
        this.messaqge = message;
    }

    public SparkSubmissionJobConfigCheckStatus getStatus(){
        return status;
    }

    public String getMessaqge(){
        return messaqge;
    }

    @Override
    public int compareTo(@NotNull SparkSubmissionJobConfigCheckResult other) {
        if (this.getStatus() == other.getStatus()) {
            // Equal
            return 0;
        } else if (this.getStatus() == SparkSubmissionJobConfigCheckStatus.Warning && other.getStatus() == SparkSubmissionJobConfigCheckStatus.Error) {
            // This is great than other, Error < Warning, put Error first
            return 1;
        } else {
            // This is less than other, Warning > Error, put Error first
            return -1;
        }
    }
}
