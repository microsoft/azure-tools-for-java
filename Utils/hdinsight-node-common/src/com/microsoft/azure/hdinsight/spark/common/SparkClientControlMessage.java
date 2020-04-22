/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogSource;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observer;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.*;
import static com.microsoft.azure.hdinsight.spark.common.log.SparkLogSource.Tool;

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
        getCtrlSubject().onNext(new SparkLogLine(Tool, Info, message));
    }

    /**
     * Send client error message from ctrlSubject
     * @param message the message to sent
     */
    default void ctrlError(final String message) {
        getCtrlSubject().onNext(new SparkLogLine(Tool, Error, message));
    }

    /**
     * Send client hyperlink message from ctrlSubject
     * @param url the message to sent
     */
    default void ctrlHyperLink(final String url) {
        getCtrlSubject().onNext(new SparkLogLine(Tool, Hyperlink, url));
    }
}
