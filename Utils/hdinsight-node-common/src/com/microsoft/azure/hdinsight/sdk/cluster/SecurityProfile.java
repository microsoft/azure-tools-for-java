/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

public class SecurityProfile {
    private String directoryType;
    private String aaddsResourceId;

    public String getDirectoryType(){
        return directoryType;
    }

    public String getAaddsResourceId(){
        return aaddsResourceId;
    }
}
