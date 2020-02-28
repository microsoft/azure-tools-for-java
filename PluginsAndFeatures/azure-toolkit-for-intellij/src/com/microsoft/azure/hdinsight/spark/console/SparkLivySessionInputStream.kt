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

import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.Session
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.Statement
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.StatementOutput
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*

abstract class SparkLivySessionInputStream(val session: Session) : InputStream(), ILogger {
    private var nextStatementId = 0
    private var statementOutputQueue: ArrayDeque<Byte>? = null

    override fun read(): Int {
        return statementOutputQueue?.pollFirst()?.toInt() ?: -1
    }

    override fun close() {
    }

    override fun available(): Int {
        if (isOutputEmpty()) {
            fetchNextStatementOutput()

            if (isOutputEmpty()) {
                Thread.sleep(1000)
            }
        }

        return statementOutputQueue?.size ?: 0
    }

    abstract fun createStatementBytesQueue(output: StatementOutput): String?

    private fun fetchNextStatementOutput() {
        val statement = Statement(session, nextStatementId)

        try {
            statementOutputQueue = statement.get()
                    .map { stm ->
                        createStatementBytesQueue(stm.output)?.let {
                            log().debug("Statement $nextStatementId result $it")
                            ArrayDeque("$it\n".toByteArray(UTF_8).toList())
                        }
                    }
                    .toBlocking()
                    .singleOrDefault(null)

            nextStatementId++
        } catch (err: Exception) {
            log().debug("Can't get the $nextStatementId output", err)
        }
    }

    private fun isOutputEmpty(): Boolean = (statementOutputQueue?.isEmpty() ?: true)
}