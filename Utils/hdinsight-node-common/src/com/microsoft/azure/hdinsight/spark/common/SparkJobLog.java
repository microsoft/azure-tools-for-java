/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import java.util.List;

public class SparkJobLog {
    private int id;
    private int from;
    private int total;
    private List<String> log;

    public int getId(){
        return id;
    }

    public int getFrom(){
        return from;
    }

    public int getTotal(){
        return total;
    }

    public List<String> getLog(){
        return log;
    }
}
