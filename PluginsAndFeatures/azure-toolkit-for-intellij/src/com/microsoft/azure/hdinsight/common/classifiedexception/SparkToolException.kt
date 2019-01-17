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

import com.intellij.openapi.application.ApplicationManager
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions.SessionNotStartException
import com.microsoft.azure.hdinsight.spark.common.SparkJobException
import com.microsoft.azure.hdinsight.spark.common.YarnDiagnosticsException
import com.microsoft.intellij.forms.ErrorMessageForm
import org.apache.commons.lang.exception.ExceptionUtils
import java.io.FileNotFoundException

const val ToolPackageSuffix: String = "com.microsoft.azure"

class SparkToolException(exp: Throwable?) : ClassifiedException(exp) {
    override val title: String = "Azure Plugin for IntelliJ Error"

    override fun handleByUser(){
        ApplicationManager.getApplication().invokeLater {
            val toolErrorDialog = ErrorMessageForm(title)
            toolErrorDialog.showErrorMessageForm(message, stackTrace)
            toolErrorDialog.show()
        }
    }
}

object SparkToolExceptionFactory : ClassifiedExceptionFactory() {
    override fun createClassifiedException(exp: Throwable?): ClassifiedException? {
        val stackTrace = if (exp != null) ExceptionUtils.getStackTrace(exp) else EmptyLog
        return if (exp !is YarnDiagnosticsException
                // Thrown from Azure blob storage SDK, refer to Issue #2580
                && exp !is IllegalArgumentException
                // Thrown from creating Livy helper session to upload artifacts,refer to Issue #2552
                && exp !is SessionNotStartException
                && exp !is SparkJobException
                && exp !is FileNotFoundException
                && stackTrace.contains(ToolPackageSuffix)) {
            SparkToolException(exp)
        } else null
    }
}