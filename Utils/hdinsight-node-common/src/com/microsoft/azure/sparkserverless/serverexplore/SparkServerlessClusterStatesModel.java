package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.net.URI;

public class SparkServerlessClusterStatesModel implements Cloneable{
    @NotNull
    private String masterState;
    @NotNull
    private String workerState;

    @NotNull
    private int masterTarget;
    @NotNull
    private int workerTarget;

    @NotNull
    private int masterRunning;
    @NotNull
    private int workerRunning;

    @NotNull
    private int masterFailed;
    @NotNull
    private int workerFailed;

    @NotNull
    private int masterOutstanding;
    @NotNull
    private int workerOutstanding;

    @NotNull
    private URI livyUri;
    @NotNull
    private URI sparkHistoryUri;
    @NotNull
    private URI sparkMasterUri;
    @NotNull
    private String clusterState;

    @NotNull
    public String getMasterState() {
        return masterState;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setMasterState(String masterState) {
        this.masterState = masterState;
        return this;
    }

    @NotNull
    public String getWorkerState() {
        return workerState;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setWorkerState(String workerState) {
        this.workerState = workerState;
        return this;
    }

    @NotNull
    public int getMasterTarget() {
        return masterTarget;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setMasterTarget(int masterTarget) {
        this.masterTarget = masterTarget;
        return this;
    }

    @NotNull
    public int getWorkerTarget() {
        return workerTarget;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setWorkerTarget(int workerTarget) {
        this.workerTarget = workerTarget;
        return this;
    }

    @NotNull
    public int getMasterRunning() {
        return masterRunning;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setMasterRunning(int masterRunning) {
        this.masterRunning = masterRunning;
        return this;
    }

    @NotNull
    public int getWorkerRunning() {
        return workerRunning;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setWorkerRunning(int workerRunning) {
        this.workerRunning = workerRunning;
        return this;
    }

    @NotNull
    public int getMasterFailed() {
        return masterFailed;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setMasterFailed(int masterFailed) {
        this.masterFailed = masterFailed;
        return this;
    }

    @NotNull
    public int getWorkerFailed() {
        return workerFailed;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setWorkerFailed(int workerFailed) {
        this.workerFailed = workerFailed;
        return this;
    }

    @NotNull
    public int getMasterOutstanding() {
        return masterOutstanding;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setMasterOutstanding(int masterOutstanding) {
        this.masterOutstanding = masterOutstanding;
        return this;
    }

    @NotNull
    public int getWorkerOutstanding() {
        return workerOutstanding;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setWorkerOutstanding(int workerOutstanding) {
        this.workerOutstanding = workerOutstanding;
        return this;
    }

    @NotNull
    public URI getLivyUri() {
        return livyUri;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setLivyUri(URI livyUri) {
        this.livyUri = livyUri;
        return this;
    }

    @NotNull
    public URI getSparkHistoryUri() {
        return sparkHistoryUri;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setSparkHistoryUri(URI sparkHistoryUri) {
        this.sparkHistoryUri = sparkHistoryUri;
        return this;
    }

    @NotNull
    public URI getSparkMasterUri() {
        return sparkMasterUri;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setSparkMasterUri(URI sparkMasterUri) {
        this.sparkMasterUri = sparkMasterUri;
        return this;
    }

    @NotNull
    public String getClusterState() {
        return clusterState;
    }

    @NotNull
    public SparkServerlessClusterStatesModel setClusterState(String clusterState) {
        this.clusterState = clusterState;
        return this;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // Here is a shadow clone, not deep clone
        return super.clone();
    }
}
