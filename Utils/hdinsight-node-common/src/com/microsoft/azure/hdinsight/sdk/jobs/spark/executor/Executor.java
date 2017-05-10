/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.sdk.jobs.spark.executor;

/**
 * Created by ltian on 5/6/2017.
 }
 */

public class Executor {
    private String id;
    private String hostPort;
    private boolean isActive;

    private long rddBlocks;
    private long memoryUsed;
    private long diskUsed;
    private int totalCores;

    private long maxTasks;
    private long activeTasks;
    private long failedTasks;
    private long completedTasks;
    private long totalTasks;

    private long totalDuration;
    private long totalGCTime;
    private long totalInputBytes;
    private long totalShuffleRead;
    private long totalShuffleWrite;
    private long maxMemory;

    private ExecutorLog executorLogs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public boolean isIsActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getRddBlocks() {
        return rddBlocks;
    }

    public void setRddBlocks(long rddBlocks) {
        this.rddBlocks = rddBlocks;
    }

    public long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public long getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(long diskUsed) {
        this.diskUsed = diskUsed;
    }

    public int getTotalCores() {
        return totalCores;
    }

    public void setTotalCores(int totalCores) {
        this.totalCores = totalCores;
    }

    public long getMaxTasks() {
        return maxTasks;
    }

    public void setMaxTasks(long maxTasks) {
        this.maxTasks = maxTasks;
    }

    public long getActiveTasks() {
        return activeTasks;
    }

    public void setActiveTasks(long activeTasks) {
        this.activeTasks = activeTasks;
    }

    public long getFailedTasks() {
        return failedTasks;
    }

    public void setFailedTasks(long failedTasks) {
        this.failedTasks = failedTasks;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(long completedTasks) {
        this.completedTasks = completedTasks;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(long totalTasks) {
        this.totalTasks = totalTasks;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public long getTotalGCTime() {
        return totalGCTime;
    }

    public void setTotalGCTime(long totalGCTime) {
        this.totalGCTime = totalGCTime;
    }

    public long getTotalInputBytes() {
        return totalInputBytes;
    }

    public void setTotalInputBytes(long totalInputBytes) {
        this.totalInputBytes = totalInputBytes;
    }

    public long getTotalShuffleRead() {
        return totalShuffleRead;
    }

    public void setTotalShuffleRead(long totalShuffleRead) {
        this.totalShuffleRead = totalShuffleRead;
    }

    public long getTotalShuffleWrite() {
        return totalShuffleWrite;
    }

    public void setTotalShuffleWrite(long totalShuffleWrite) {
        this.totalShuffleWrite = totalShuffleWrite;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public ExecutorLog getExecutorLogs() {
        return executorLogs;
    }

    public void setExecutorLogs(ExecutorLog executorLog) {
        this.executorLogs = executorLog;
    }
}
