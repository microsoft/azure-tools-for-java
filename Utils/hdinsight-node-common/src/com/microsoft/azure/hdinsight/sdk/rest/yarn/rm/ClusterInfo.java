/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
