/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects;

public enum HDInsightExternalSystem {
    MAVEN("Maven"),
    SBT("SBT");

    private final String displayName;

    HDInsightExternalSystem(String name) {
        displayName = name;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
