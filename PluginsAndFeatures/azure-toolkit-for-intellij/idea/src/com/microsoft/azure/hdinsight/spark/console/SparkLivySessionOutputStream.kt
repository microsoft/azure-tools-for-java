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

package com.microsoft.azure.hdinsight.spark.console

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.Session
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.StatementExecutionError
import org.apache.commons.io.output.ByteArrayOutputStream
import java.nio.charset.Charset

class SparkLivySessionOutputStream(val session: Session) : ByteArrayOutputStream(), ILogger {
    override fun flush() {
        // Send the buffered statements into Livy services
        if (!session.isStarted) {
            throw SparkConsoleExceptions.LivyNotConnected("The Livy session to ${session.name} is not connected")
        }

        val codes = toString(Charset.defaultCharset())
        log().debug("Send those codes to Livy: $codes")

        session.runCodes(codes)
                .doOnEach { reset() }
                .toBlocking()
                .subscribe(
                        { result ->
                            log().debug("Livy running results: ${ObjectMapper().writeValueAsString(result)}")
                        },
                        { err -> when (err.cause) {
                            is StatementExecutionError -> log().debug(err.message)
                            else -> throw SparkConsoleExceptions.LivySessionExecuteError(
                                    "Got the code `${codes.split(
                                            "\n").first()}` execution error:",
                                    err.cause ?: err)
                        }}
                )
    }
}