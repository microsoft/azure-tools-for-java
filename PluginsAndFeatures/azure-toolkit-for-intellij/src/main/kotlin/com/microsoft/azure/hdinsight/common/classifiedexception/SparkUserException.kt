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

import com.microsoft.azure.datalake.store.ADLException
import com.microsoft.azure.hdinsight.spark.common.SparkJobException
import com.microsoft.azure.hdinsight.spark.common.YarnDiagnosticsException
import com.microsoft.azuretools.adauth.AuthException
import com.microsoft.azuretools.telemetrywrapper.ErrorType
import java.io.FileNotFoundException

class SparkUserException(exp: Throwable?) : ClassifiedException(exp) {
    override val title: String = "Spark User Error"
    override val errorType = ErrorType.userError

    override fun handleByUser() {
    }
}

object SparkUserExceptionFactory : ClassifiedExceptionFactory() {
    override fun createClassifiedException(exp: Throwable?): ClassifiedException? {
        return if (exp is YarnDiagnosticsException
                // Throw from wrong class name ,refer to issue 1827 and 2466
                || exp is SparkJobException
                || exp is IllegalArgumentException
                || exp is FileNotFoundException) {
            SparkUserException(exp)
        } else if (exp is ADLException && exp.httpResponseCode == 403) {
            val hintMsg = "\nPlease make sure user has RWX permissions for the storage account"
            SparkUserException(ADLException("${exp.remoteExceptionMessage}$hintMsg"))
        } else if (exp is AuthException) {
            val cause = if (exp.error == "invalid_grant") {
                val hintMsg = String.format("\nPlease check whether have enough permission to access the cluster")
                AuthException("${exp.errorMessage}$hintMsg")
            } else exp

            SparkUserException(cause)
        } else null
    }
}