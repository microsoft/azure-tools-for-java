/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

public enum SparkBatchJobState {
    NOT_STARTED("not_started"),
    STARTING("starting"),
    RECOVERING("recovering"),
    IDLE("idle"),
    RUNNING("running"),
    BUSY("busy"),
    SHUTTING_DOWN("shutting_down"),
    ERROR("error"),
    DEAD("dead"),
    SUCCESS("success");

    private final String state;

    SparkBatchJobState(String state) {
        this.state = state;
    }


    @Override
    public String toString() {
        return state;
    }
}
