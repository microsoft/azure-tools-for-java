/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.entities;

import java.util.UUID;

/**
 * Created by vlashch on 8/19/16.
 */
public class RoleAssignment {

    public Properties properties = new Properties();

    public class Properties {
        public String roleDefinitionId;
        public UUID principalId;
    }
}
