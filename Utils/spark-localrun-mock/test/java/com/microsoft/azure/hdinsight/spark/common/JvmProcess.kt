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

import java.io.File
import java.util.*
import kotlin.collections.HashMap

open class JvmProcess {
    open var workingDirectory: String = System.getProperty("user.dir")

    private var classpath: String = System.getProperty("java.class.path")

    private var jvm: String = File(System.getProperty("java.home")).resolve( "bin").resolve("java").path

    val additionalEnv = HashMap<String, String>()

    private var stdOut: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT
    private var stdErr: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT

    fun createProcess(jvmOptions: String, mainClass: Class<*>, arguments: Array<String>): ProcessBuilder =
            createProcess(jvmOptions, mainClass.canonicalName, arguments)

    open fun createProcess(jvmOptions: String, mainClass: String, arguments: Array<String>): ProcessBuilder {
        val options = jvmOptions.split(" ")
                                .filterNot { it.isEmpty() }

        val command = ArrayList<String>().apply {
            add(jvm)
            addAll(options)
            add(mainClass)
            addAll(arguments)
        }

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(File(workingDirectory))

        val env = processBuilder.environment()
        env["CLASSPATH"] = classpath
        env.putAll(additionalEnv)

        processBuilder.redirectOutput(stdOut)
        processBuilder.redirectError(stdErr)

        return processBuilder
    }
}