/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.yarn.rm;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * Created by ltian on 5/6/2017.
 */
public class ClusterInfo implements IConvertible
{
    private String id;

    private String hadoopBuildVersion;

    private String haState;

    private String hadoopVersionBuiltOn;

    private String hadoopVersion;

    private String startedOn;

    private String resourceManagerVersion;

    private String haZooKeeperConnectionState;

    private String state;

    private String rmStateStoreName;

    private String resourceManagerVersionBuiltOn;

    private String resourceManagerBuildVersion;

    public String getId ()
    {
        return id;
    }

    public void setId (String id)
    {
        this.id = id;
    }

    public String getHadoopBuildVersion ()
    {
        return hadoopBuildVersion;
    }

    public void setHadoopBuildVersion (String hadoopBuildVersion)
    {
        this.hadoopBuildVersion = hadoopBuildVersion;
    }

    public String getHaState ()
    {
        return haState;
    }

    public void setHaState (String haState)
    {
        this.haState = haState;
    }

    public String getHadoopVersionBuiltOn ()
    {
        return hadoopVersionBuiltOn;
    }

    public void setHadoopVersionBuiltOn (String hadoopVersionBuiltOn)
    {
        this.hadoopVersionBuiltOn = hadoopVersionBuiltOn;
    }

    public String getHadoopVersion ()
    {
        return hadoopVersion;
    }

    public void setHadoopVersion (String hadoopVersion)
    {
        this.hadoopVersion = hadoopVersion;
    }

    public String getStartedOn ()
    {
        return startedOn;
    }

    public void setStartedOn (String startedOn)
    {
        this.startedOn = startedOn;
    }

    public String getResourceManagerVersion ()
    {
        return resourceManagerVersion;
    }

    public void setResourceManagerVersion (String resourceManagerVersion)
    {
        this.resourceManagerVersion = resourceManagerVersion;
    }

    public String getHaZooKeeperConnectionState ()
    {
        return haZooKeeperConnectionState;
    }

    public void setHaZooKeeperConnectionState (String haZooKeeperConnectionState)
    {
        this.haZooKeeperConnectionState = haZooKeeperConnectionState;
    }

    public String getState ()
    {
        return state;
    }

    public void setState (String state)
    {
        this.state = state;
    }

    public String getRmStateStoreName ()
    {
        return rmStateStoreName;
    }

    public void setRmStateStoreName (String rmStateStoreName)
    {
        this.rmStateStoreName = rmStateStoreName;
    }

    public String getResourceManagerVersionBuiltOn ()
    {
        return resourceManagerVersionBuiltOn;
    }

    public void setResourceManagerVersionBuiltOn (String resourceManagerVersionBuiltOn)
    {
        this.resourceManagerVersionBuiltOn = resourceManagerVersionBuiltOn;
    }

    public String getResourceManagerBuildVersion ()
    {
        return resourceManagerBuildVersion;
    }

    public void setResourceManagerBuildVersion (String resourceManagerBuildVersion)
    {
        this.resourceManagerBuildVersion = resourceManagerBuildVersion;
    }
}
