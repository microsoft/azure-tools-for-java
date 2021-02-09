/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

public class ClusterRawInfo {
    private String id;
    private String name;
    private String type;
    private String location;
    private String etag;
    private ClusterProperties properties;

    public String getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getType(){
        return type;
    }

    public String getLocation(){
        return location;
    }

    public String getEtag(){
        return etag;
    }

    public ClusterProperties getProperties() {
        return properties;
    }
}
