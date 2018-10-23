package com.microsoft.azure.hdinsight.common.classifiedexception

class UnclassifiedException(exp: Throwable?) : ClassifiedException(exp) {
    override val title: String = "Unclassified Error"
}