/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

public class ComponentVersion {
    /*
        This is a walking around to fix getting spark version problem.
        'spark', 'Spark', 'SPARK' can be expected in different kind of HDInsight REST API response.
     */
    private String spark;
    private String Spark;
    private String SPARK;

    public String getSpark() {
        return this.spark != null ? this.spark : (this.SPARK != null ? this.SPARK : this.Spark);
    }
}
