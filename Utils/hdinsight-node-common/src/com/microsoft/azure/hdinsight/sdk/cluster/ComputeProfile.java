/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import java.util.List;

public class ComputeProfile {

    private List<Role> roles;

    public List<Role> getRoles(){
        return roles;
    }
}
