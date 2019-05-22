package com.microsoft.azure.hdinsight.spark.ui.filesystem

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.microsoft.azuretools.azurecommons.helpers.Nullable
import java.net.URI

open class AdlsGen2VirtualFile(private val myPath: String, private val myIsDirectory: Boolean, private val myFileSystem: VirtualFileSystem) : AzureStorageVirtualFile() {
    private var parent: VirtualFile? = null
    override fun getPath() = myPath
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

    override fun getChildren(): Array<VirtualFile> = myLazyChildren

    private val myLazyChildren: Array<VirtualFile> by lazy {
        var childrenList: MutableList<VirtualFile> = arrayListOf()
        if (this.isDirectory) {
            val gen2FileSystem = fileSystem as ADLSGen2FileSystem
            gen2FileSystem.op.list(gen2FileSystem.restApiRoot, gen2FileSystem.op.uriHelper.getDirectoryParam(URI.create(this.path)))
                    .map<AdlsGen2VirtualFile> { path ->
                        AdlsGen2VirtualFile(gen2FileSystem.root.resolve(path.name).toString(),
                                path.isDirectory!!, this.fileSystem)
                    }
                    .doOnNext { file ->
                        childrenList.add(file)
                        file.setParent(this)
                    }.toBlocking().subscribe({},
                            { err -> log().warn("Listing files encounters exception", err) })
            childrenList.toTypedArray<VirtualFile>()
        } else {
            childrenList.toTypedArray<VirtualFile>()
        }
    }
}