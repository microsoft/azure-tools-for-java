/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.entities;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by vlashch on 8/17/16.
 */
public class Application {
    public boolean availableToOtherTenants;
    public String displayName;
    public String homepage;
    public List<String> identifierUris = new LinkedList<String>();
    public List<PasswordCredentials> passwordCredentials = new LinkedList<PasswordCredentials>();

}


