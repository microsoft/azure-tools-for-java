/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.common.classifiedexception

const val EmptyLog: String = "Exception without detail message"

abstract class ClassifiedExceptionFactory {
    companion object {
        fun createClassifiedException(exp: Throwable?): ClassifiedException {
            val rootCause: Throwable? = (exp?.cause) ?: exp
            return SparkServiceExceptionFactory.createClassifiedException(rootCause)
                    ?: SparkToolExceptionFactory.createClassifiedException(rootCause)
                    ?: SparkUserExceptionFactory.createClassifiedException(rootCause)
                    // Keep Unclassified Exception at bottom as the default return
                    ?: UnclassifiedException(rootCause)
        }
    }

    abstract fun createClassifiedException(exp: Throwable?): ClassifiedException?
}