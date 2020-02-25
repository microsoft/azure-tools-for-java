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

class ReportableFeatureRequest(shortMessage: String) : Reportable(shortMessage) {
    private val template = listOf(
            "####  Is your feature request related to a problem? Please describe",
            "A clear and concise description of what the problem is. Ex. I'm always frustrated when [...]",
            "####  Describe the solution you'd like",
            "A clear and concise description of what you want to happen.",
            "#### Additional context",
            "Add any other context or screenshots about the feature request here."
    )

    override fun getTitleTags(): Set<String> {
        return setOf("IntelliJ", "ReportedByUser", "feature-request")
    }

    override fun getBody(): String {
        return template.joinToString("\n\n")
    }
}
