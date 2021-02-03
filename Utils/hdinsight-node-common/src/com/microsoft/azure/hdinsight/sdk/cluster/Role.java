/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

public class Role {
    private String name;
    private int targetInstanceCount;
    private HardwareProfile hardwareProfile;

    public String getName(){
        return name;
    }

    public int getTargetInstanceCount(){
        return targetInstanceCount;
    }

    public HardwareProfile getHardwareProfile() {
        return hardwareProfile;
    }
}

