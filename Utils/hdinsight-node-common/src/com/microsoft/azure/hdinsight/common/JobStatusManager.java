/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

public class JobStatusManager {
    private boolean isJobKilled = false;
    private boolean isApplicationGenerated = false;
    private String applicationId;
    private boolean isRunning = false;

    public void resetJobStateManager() {
        isJobKilled = false;
        isApplicationGenerated = false;
    }

    public boolean isJobRunning() {
        return isRunning;
    }

    public void setJobRunningState(boolean isRun) {
        isRunning = isRun;
    }

    public boolean isJobKilled() {
        return isJobKilled;
    }

    public void setJobKilled() {
        isJobKilled = true;
        isRunning = false;
    }

    public boolean isApplicationGenerated(){
        return isApplicationGenerated;
    }

    public void setApplicationIdGenerated(){
        isApplicationGenerated = true;
    }

    public void setApplicationId(String applicationId){
        this.applicationId = applicationId;
    }

    public String getApplicationId(){
        return applicationId;
    }
}
