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
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import java.net.URI
import java.nio.charset.StandardCharsets

class GithubIssue<T : Reportable>(private val reportable: T) {
    private val plugin = reportable.plugin
    private val labels = mutableSetOf("IntelliJ")

    private val pluginRepo: URI
        get() {
            val url = if (plugin.url.endsWith("/")) plugin.url else plugin.url + "/"

            return URI.create(url)
        }

    private fun getRequestUrl(): String {
        // A very simple implement with embedded Github Issue parameters
        // into the browser URL (GET request), so there is a limitation
        // of 2083 char-length.
        //
        // To support a bigger issue body, please implement a RESTful API
        // version request.

        return StringUtils.left(pluginRepo.resolve("issues/new?" + URLEncodedUtils.format(listOf(
                BasicNameValuePair("title", reportable.getTitle()),
                BasicNameValuePair("labels", labels.joinToString(",")),
                BasicNameValuePair("body", reportable.getBody())
        ), StandardCharsets.UTF_8)).toString(), 2083)  // 2083 URL max length
        .replace("""%[\d\w]?$""", "")                  // remove ending uncompleted escaped chars
    }

    fun report() {
        DefaultLoader.getIdeHelper().openLinkInBrowser(getRequestUrl())
    }

    fun withLabel(label: String): GithubIssue<T> {
        labels.add(label)

        return this
    }
}