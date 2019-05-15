package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.impl.http.HttpFileSystemImpl;
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile;
import com.intellij.openapi.vfs.impl.http.RemoteFileInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AdlsGen2VirtualFile extends HttpVirtualFile {
    private String name;
    private String path;
    private VirtualFileSystem fileSystem;
    private boolean isDirectory;
    private String prefix;
    private List<VirtualFile> children = new ArrayList<>();
    private VirtualFile parent;
    public static VirtualFile empty = new AdlsGen2VirtualFile("", false, "", new HttpFileSystemImpl());

    public AdlsGen2VirtualFile(String path, boolean isDirectory, String prefix, VirtualFileSystem fileSystem) {
        this.name = path.substring(path.lastIndexOf("/") + 1);
        this.path = path;
        this.isDirectory = isDirectory;
        this.prefix = prefix;
        this.fileSystem = fileSystem;
    }

    @Nullable
    @Override
    public RemoteFileInfo getFileInfo() {
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        return this.fileSystem;
    }

    @NotNull
    @Override
    public String getPath() {
        return prefix != null ? String.format("%s/%s", this.prefix, this.path) : this.path;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return this.isDirectory;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    @Nullable
    public VirtualFile getParent() {
        return this.parent;
    }

    public void setParent(VirtualFile parent) {
        this.parent = parent;
    }

    @Override
    public VirtualFile[] getChildren() {
        return this.children.toArray(new AdlsGen2VirtualFile[0]);
    }

    public void addChildren(AdlsGen2VirtualFile vf) {
        this.children.add(vf);
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        return null;
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray() throws IOException {
        return new byte[0];
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }
}
