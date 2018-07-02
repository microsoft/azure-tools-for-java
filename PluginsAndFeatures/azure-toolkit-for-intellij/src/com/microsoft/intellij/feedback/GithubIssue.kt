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

import com.microsoft.tooling.msservices.components.DefaultLoader
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import java.net.URI
import java.nio.charset.StandardCharsets

class GithubIssue(shortMessage: String, detailMessage: String) : ReportableError(shortMessage, detailMessage) {
    private val labels = mutableSetOf("IntelliJ", "bug")

    private val pluginRepo: URI
        get() {
            val url = if (plugin.url.endsWith("/")) plugin.url else plugin.url + "/"

            return URI.create(url)
        }

    private fun getRequestUrl(): String {
        return pluginRepo.resolve("issues/new?" + URLEncodedUtils.format(listOf(
                BasicNameValuePair("title", getTitle()),
                BasicNameValuePair("labels", labels.joinToString(",")),
                BasicNameValuePair("body", getBody())
        ), StandardCharsets.UTF_8)).toString()
    }

    override fun report() {
        DefaultLoader.getIdeHelper().openLinkInBrowser(getRequestUrl())
    }

    fun withLabel(label: String): GithubIssue {
        labels.add(label)

        return this
    }
}