/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.entities;

import java.util.List;
import java.util.UUID;

/**
 * Created by vlashch on 8/26/16.
 */

// custom

public class SrvPriData {
    UUID appId;
    UUID spId;
    List<RoleData> roles;

    public static class RoleData {

    }
}
