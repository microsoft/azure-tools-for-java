package com.microsoft.azure.cosmosspark.serverexplore;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.net.URI;

public class CosmosSparkClusterStatesModel implements Cloneable{
    @NotNull
    private String masterState = "";
    @NotNull
    private String workerState = "";

    private int masterTarget;
    private int workerTarget;

    private int masterRunning;
    private int workerRunning;

    private int masterFailed;
    private int workerFailed;

    private int masterOutstanding;
    private int workerOutstanding;

    @Nullable
    private URI sparkHistoryUri;
    @Nullable
    private URI sparkMasterUri;
    @NotNull
    private String clusterState = "";
    @NotNull
    private String clusterID = "";

    @NotNull
    public String getMasterState() {
        return masterState;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setMasterState(@NotNull String masterState) {
        this.masterState = masterState;
        return this;
    }

    @NotNull
    public String getWorkerState() {
        return workerState;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setWorkerState(@NotNull String workerState) {
        this.workerState = workerState;
        return this;
    }

    public int getMasterTarget() {
        return masterTarget;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setMasterTarget(int masterTarget) {
        this.masterTarget = masterTarget;
        return this;
    }

    public int getWorkerTarget() {
        return workerTarget;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setWorkerTarget(int workerTarget) {
        this.workerTarget = workerTarget;
        return this;
    }

    public int getMasterRunning() {
        return masterRunning;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setMasterRunning(int masterRunning) {
        this.masterRunning = masterRunning;
        return this;
    }

    public int getWorkerRunning() {
        return workerRunning;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setWorkerRunning(int workerRunning) {
        this.workerRunning = workerRunning;
        return this;
    }

    public int getMasterFailed() {
        return masterFailed;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setMasterFailed(int masterFailed) {
        this.masterFailed = masterFailed;
        return this;
    }

    public int getWorkerFailed() {
        return workerFailed;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setWorkerFailed(int workerFailed) {
        this.workerFailed = workerFailed;
        return this;
    }

    public int getMasterOutstanding() {
        return masterOutstanding;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setMasterOutstanding(int masterOutstanding) {
        this.masterOutstanding = masterOutstanding;
        return this;
    }

    public int getWorkerOutstanding() {
        return workerOutstanding;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setWorkerOutstanding(int workerOutstanding) {
        this.workerOutstanding = workerOutstanding;
        return this;
    }

    @Nullable
    public URI getSparkHistoryUri() {
        return sparkHistoryUri;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setSparkHistoryUri(@Nullable URI sparkHistoryUri) {
        this.sparkHistoryUri = sparkHistoryUri;
        return this;
    }

    @Nullable
    public URI getSparkMasterUri() {
        return sparkMasterUri;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setSparkMasterUri(@Nullable URI sparkMasterUri) {
        this.sparkMasterUri = sparkMasterUri;
        return this;
    }

    @NotNull
    public String getClusterState() {
        return clusterState;
    }

    @NotNull
    public CosmosSparkClusterStatesModel setClusterState(@NotNull String clusterState) {
        this.clusterState = clusterState;
        return this;
    }

    @NotNull
    public String getClusterID() {
        return clusterID;
    }

    public CosmosSparkClusterStatesModel setClusterID(@NotNull String clusterID) {
        this.clusterID = clusterID;
        return this;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // Here is a shadow clone, not deep clone
        return super.clone();
    }
}
