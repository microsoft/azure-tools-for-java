/**
 * Copyright (c) 2019-2021 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.run

import com.google.gson.*
import com.intellij.openapi.diagnostic.Logger
import java.io.File

object HostJsonPatcher {

    private val logger = Logger.getInstance(HostJsonPatcher::class.java)

    private const val FUNCTION_PROPERTY_NAME = "functions"

    private fun determineHostJsonFile(workingDirectory: String): File? {
        val candidates = listOf(
                File(workingDirectory, "host.json"),
                File(workingDirectory, "../host.json"),
                File(workingDirectory, "../../host.json")
        )

        return candidates.firstOrNull { it.exists() }?.canonicalFile
    }

    fun tryPatchHostJsonFile(workingDirectory: String, functionNames: String) =
            tryPatchHostJsonFile(
                    hostJsonFile = determineHostJsonFile(workingDirectory),
                    functionNames = functionNames.split(',', ';', '|', ' '))

    private fun tryPatchHostJsonFile(hostJsonFile: File?, functionNames: List<String>) {

        if (hostJsonFile == null || !hostJsonFile.exists()) {
            logger.warn("Could not find host.json file to patch.")
            return
        }

        val functions = functionNames
                .map { it.trim() }
                .filter { it.isNotBlank() }

        if (functions.isEmpty()) {
            logger.info("Skip patching " + hostJsonFile.absolutePath + " - no function names were specified.")
            return
        }

        logger.info("Patching " + hostJsonFile.absolutePath + " with function names: ${functions.joinToString(", ")}")
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val hostJson = gson.fromJson(hostJsonFile.readText(), JsonElement::class.java).asJsonObject

            val functionsArray = JsonArray()
            functions.forEach { functionsArray.add(JsonPrimitive(it)) }
            hostJson.add(FUNCTION_PROPERTY_NAME, functionsArray)

            hostJsonFile.writeText(gson.toJson(hostJson))
        } catch (e: JsonParseException) {
            logger.error(e)
        }
    }
}
