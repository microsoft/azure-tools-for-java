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
import java.io.Reader
import java.util.*

class SparkLivySessionInputReader(val session: Session, val type: OutputType) : Reader(), ILogger {
    var nextStatementId = 0

    private var statementOutputQueue: ArrayDeque<Char>? = null

    enum class OutputType {
        STDOUT, STDERR
    }

    override fun close() {
    }

    // Can block
    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (statementOutputQueue == null || statementOutputQueue!!.isEmpty()) {
            // Fetch statement output
            val statement = Statement(session, nextStatementId)

            try {
                statementOutputQueue = statement.get()
                        .map { stm -> stm.output.data[type.name]?.let { ArrayDeque(it.toList()) } }
                        .toBlocking()
                        .singleOrDefault(null)

                nextStatementId++
            } catch (err: Exception) {
                log().warn("Can't get the $nextStatementId output", err)

                if (session.isStop) {
                    return -1
                }
            }
        }

        val actualSizeToRead = minOf(statementOutputQueue?.size ?: 0, len, cbuf.size - off)

        if (actualSizeToRead <= 0) {
            return 0
        }

        for (i in off..off + actualSizeToRead) {
            cbuf[i] = statementOutputQueue!!.pollFirst()
        }

        return actualSizeToRead
    }
}