/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

public class HDInsightLoader {

    private static HDInsightHelper hdInsightHelper;

    public static HDInsightHelper getHDInsightHelper() {
        return hdInsightHelper;
    }

    public static void setHHDInsightHelper(HDInsightHelper helper) {
        hdInsightHelper = helper;
    }
}
