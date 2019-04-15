/*
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

package com.microsoft.azure.hdinsight.spark.common

import mockit.integration.junit4.JMockit
import java.io.File
import kotlin.test.fail

class SparkLocalJvmProcess : JvmProcess() {
    private val targetDir = File(javaClass.protectionDomain.codeSource.location.file).parentFile
    private val dataRootDir: File = targetDir.resolve("data")

    val userDefaultDir: String = dataRootDir.resolve("__default__").resolve("user").resolve("current").path

    override var workingDirectory: String = userDefaultDir

    override fun createProcess(jvmOptions: String, mainClass: String, arguments: Array<String>): ProcessBuilder {
        val jMockitClass = JMockit::class.java
        val uri = jMockitClass.getResource(
                "/${jMockitClass.canonicalName.replace('.', '/')}.class")
        val jarPathRegex = """(zip:|jar:file:)(/.*)!/(.*)""".toRegex()

        additionalEnv["HADOOP_HOME"] = targetDir.resolve("tools").resolve("winutils").resolve("hadoop-2.7.1").path

        return jarPathRegex.matchEntire(uri.toString())
                           ?.groups
                           ?.get(2)
                           ?.let { File(it.value) }
                           ?.let {
                               val newJvmOptions = "-javaagent:$it $jvmOptions" +
                                       if (isDebugEnabled)
                                           " -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
                                       else
                                           ""

                               super.createProcess(newJvmOptions, mainClass, arguments)
                           }
                ?: fail("Can't find JMockit jar file in classpaths")
    }

    var isDebugEnabled = false
}