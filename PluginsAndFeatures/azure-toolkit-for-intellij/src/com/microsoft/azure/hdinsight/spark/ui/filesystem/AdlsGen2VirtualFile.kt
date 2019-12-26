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

package com.microsoft.azure.hdinsight.spark.ui.filesystem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.microsoft.azure.hdinsight.common.AbfsUri
import com.microsoft.azuretools.azurecommons.helpers.Nullable

open class AdlsGen2VirtualFile(val abfsUri: AbfsUri, private val myIsDirectory: Boolean, private val myFileSystem: VirtualFileSystem) : AzureStorageVirtualFile() {
    private var parent: VirtualFile? = null
    override fun getPath(): String = abfsUri.uri.path
    override fun getName(): String {
        return path.substring(path.lastIndexOf("/") + 1)
    }

    override fun getFileSystem() = myFileSystem

    override fun isDirectory() = myIsDirectory

    @Nullable
    override fun getParent(): VirtualFile? {
        return this.parent
    }

    override fun setParent(parent: VirtualFile) {
        this.parent = parent
    }

    override fun getChildren(): Array<VirtualFile>? = myLazyChildren

    override fun getUrl(): String {
        return abfsUri.uri.toString()
    }

    private val myLazyChildren: Array<VirtualFile>? by lazy {
        (myFileSystem as? ADLSGen2FileSystem)?.listFiles(this)
    }
}