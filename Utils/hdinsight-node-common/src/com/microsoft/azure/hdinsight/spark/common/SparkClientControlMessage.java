/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observer;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Error;
import static com.microsoft.azure.hdinsight.common.MessageInfoType.*;
import static com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine.TOOL;

public interface SparkClientControlMessage {
    /**
     * Get the job control messages observable
     *
     * @return the job control message type and content pair observable
     */
    @NotNull
    Observer<SparkLogLine> getCtrlSubject();

    /**
     * Send a line of log from ctrlSubject
     * @param logSource source of the log
     * @param messageInfoType message info type
     * @param rawLog a line of raw log
     */
    default void ctrlLog(final String logSource,
                         final MessageInfoType messageInfoType,
                         final String rawLog) {
        getCtrlSubject().onNext(new SparkLogLine(logSource, messageInfoType, rawLog));
    }

    /**
     * Send client info message from ctrlSubject
     * @param message the message to sent
     */
    default void ctrlInfo(final String message) {
        getCtrlSubject().onNext(new SparkLogLine(TOOL, Info, message));
    }

    /**
     * Send client error message from ctrlSubject
     * @param message the message to sent
     */
    default void ctrlError(final String message) {
        getCtrlSubject().onNext(new SparkLogLine(TOOL, Error, message));
    }

    /**
     * Send client hyperlink message from ctrlSubject
     * @param url the message to sent
     */
    default void ctrlHyperLink(final String url) {
        getCtrlSubject().onNext(new SparkLogLine(TOOL, Hyperlink, url));
    }
}
