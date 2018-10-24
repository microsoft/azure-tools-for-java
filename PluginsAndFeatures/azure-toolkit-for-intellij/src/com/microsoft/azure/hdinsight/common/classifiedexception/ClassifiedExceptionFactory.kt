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