package com.microsoft.azure.hdinsight.common.classifiedexception

import java.io.IOException

class SparkServiceException(exp: Throwable?) : ClassifiedException(exp) {
    override val title: String = "Spark Service Error"
}

object SparkServiceExceptionFactory : ClassifiedExceptionFactory() {
    override fun createClassifiedException(exp: Throwable?): ClassifiedException? {
        return if (exp is IOException) SparkServiceException(exp) else null
    }
}
