/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common.log;

import com.microsoft.azure.hdinsight.common.MessageInfoType;

public class SparkLogLine {
    public static final String TOOL = "azuretool";
    public static final String LIVY = "livy";
    public static final String SPARK_DRIVER_STDERR = "driver.stderr";

    private final String logSource;
    private final MessageInfoType messageInfoType;
    private final String rawLog;

    public SparkLogLine(final String logSource,
                        final MessageInfoType messageInfoType,
                        final String rawLog) {
        this.logSource = logSource;
        this.messageInfoType = messageInfoType;
        this.rawLog = rawLog;
    }

    public MessageInfoType getMessageInfoType() {
        return messageInfoType;
    }

    public String getLogSource() {
        return logSource;
    }

    public String getRawLog() {
        return rawLog;
    }
}
