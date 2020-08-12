/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure

import org.apache.http.client.utils.URIUtils
import java.net.URI
import java.util.*

fun String?.orWhenNullOrEmpty(another: String): String = if (this.isNullOrEmpty()) {
    another
} else {
    this
}

fun String?.isValidUrl(): Boolean {
    if (this.isNullOrEmpty()) return false

    return try {
        val uri = URI.create(this)
        URIUtils.extractHost(uri) != null
    } catch (_: Exception) {
        false
    }
}

fun String?.isValidUUID(): Boolean {
    if (this.isNullOrEmpty()) return false

    return try {
        UUID.fromString(this)
        this.length == 36
    } catch (_: Exception) {
        false
    }
}