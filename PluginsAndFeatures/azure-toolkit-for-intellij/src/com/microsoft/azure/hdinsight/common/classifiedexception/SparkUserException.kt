package com.microsoft.azure.hdinsight.common.classifiedexception

import com.microsoft.azure.hdinsight.spark.common.YarnDiagnosticsException

class SparkUserException(exp: Throwable?) : ClassifiedException(exp) {
    override val title: String = "Spark User Error"
}

object SparkUserExceptionFactory : ClassifiedExceptionFactory() {
    override fun createClassifiedException(exp: Throwable?): ClassifiedException? {
        return if (exp is YarnDiagnosticsException) SparkUserException(exp) else null
    }
}