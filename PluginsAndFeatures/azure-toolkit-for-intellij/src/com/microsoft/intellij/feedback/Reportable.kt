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

package com.microsoft.intellij.feedback

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId

open class Reportable(private val shortMessage: String) {
    val plugin = PluginManager.getPlugin(PluginId.getId("com.microsoft.tooling.msservices.intellij.azure"))!!
    private val appInfo = ApplicationInfo.getInstance()

    private val platformInfo = mutableMapOf<String, String>(
            "IntelliJ build version" to "${appInfo.fullVersion} ${appInfo.build}",
            "JDK" to "${System.getProperty("java.vendor")} ${System.getProperty("java.version")}",
            "Plugin version" to plugin.version
    )

    protected open val detailInfo get() = mapOf<String, String>()

    private val additionalInfo = mutableMapOf<String, String>()

    open fun getTitleTags(): Set<String> {
        return setOf("IntelliJ", "ReportedByUser")
    }

    open fun getTitle(): String {
        val tags = getTitleTags()
                .map { "[$it]" }
                .reduce { l, r -> "$l$r"}     // Output as: [Tag1][Tag2]

        return "$tags $shortMessage"
    }

    open fun getBody(): String {
        return (platformInfo + additionalInfo + detailInfo)
                .map { "${it.key}: ${it.value}" }
                .reduce { l, r -> "$l\n$r"}
    }

    open fun with(key: String, value: String): Reportable {
        additionalInfo[key] = value

        return this
    }
}