package com.microsoft.azure.hdinsight.common.classifiedexception

import com.microsoft.azure.hdinsight.common.logger.ILogger
import org.apache.commons.lang.exception.ExceptionUtils

abstract class ClassifiedException(exp: Throwable?) : Throwable(exp), ILogger {
    abstract val title: String
    override val message: String
        get() = cause?.message ?: EmptyLog

    fun getStackTrace(): String {
        return if (cause != null) ExceptionUtils.getStackTrace(cause) else message
    }

    fun logStackTrace() {
        log().warn("$title: $stackTrace")
    }
}