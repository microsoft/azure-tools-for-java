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
import java.lang.reflect.Method

class SparkScalaPluginDelegate(val sparkScalaClassName: String) : ILogger {
    val sparkScalaObj : Any by lazy { sparkScalaClass!!.newInstance() }

    val isEnabled = ScalaPluginUtils.isScalaPluginEnabled && sparkScalaClass != null

    private val sparkScalaClass: Class<*>?
        get() = try {
            val clazz = Class.forName(sparkScalaClassName)

            log().debug("class ${clazz.canonicalName} is loaded from ${clazz.protectionDomain.codeSource.location}")
            clazz
        } catch (err: Throwable) {
            log().debug("Class $sparkScalaClassName is not found", err)
            null
        }

    fun getMethod(methodName: String, vararg args: Class<*>): Method? = try {
        sparkScalaClass?.getMethod(methodName, *args)
    } catch (err: Throwable) {
        log().debug("Method `$methodName` is not found", err)
        null
    }

}

