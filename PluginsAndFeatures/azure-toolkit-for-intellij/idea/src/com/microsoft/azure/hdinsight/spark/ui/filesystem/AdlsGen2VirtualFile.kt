package com.microsoft.azure.hdinsight.spark.ui.filesystem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.microsoft.azuretools.azurecommons.helpers.NotNull
import com.microsoft.azuretools.azurecommons.helpers.Nullable
import java.net.URI

open class AdlsGen2VirtualFile(val uri: URI, private val myIsDirectory: Boolean, private val myFileSystem: VirtualFileSystem) : AzureStorageVirtualFile() {
    private var parent: VirtualFile? = null
    override fun getPath() = uri.path
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
        return uri.toString()
    }

    private val myLazyChildren: Array<VirtualFile>? by lazy {
        (myFileSystem as? ADLSGen2FileSystem)?.listFiles(this)
    }
}