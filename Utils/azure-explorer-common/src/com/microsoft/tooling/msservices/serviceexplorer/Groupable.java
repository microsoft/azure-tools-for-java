/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

public interface Groupable {

    int DEFAULT_GROUP = 100;
    int MAINTENANCE_GROUP = 200;
    int DIAGNOSTIC_GROUP = 300;

    // group with lower number will in front sear
    default int getGroup() {
        return DEFAULT_GROUP;
    }
}
