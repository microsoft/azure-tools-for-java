/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.model;

public enum ReplicationTypes {
    Standard_ZRS,
    Standard_LRS,
    Standard_GRS,
    Standard_RAGRS,
    Premium_LRS;

    public String getDescription() {
        switch (this) {
            case Standard_ZRS:
                return "Zone-Redundant";
            case Standard_GRS:
                return "Geo-Redundant";
            case Standard_LRS:
                return "Locally Redundant";
            case Standard_RAGRS:
                return "Read Access Geo-Redundant";
            case Premium_LRS:
                return "Locally Redundant";
        }

        return super.toString();
    }
}
