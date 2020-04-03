package com.microsoft.intellij.helpers.rediscache;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.helpers.UIHelperImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Created by sheche on 2017/7/4.
 */
public class RedisCacheExplorerProvider implements FileEditorProvider, DumbAware {

    public static final String TYPE = "REDIS_EXPLORER";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(TYPE);
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        String sid = virtualFile.getUserData(UIHelperImpl.SUBSCRIPTION_ID);
        String id = virtualFile.getUserData(UIHelperImpl.RESOURCE_ID);
        RedisCacheExplorer redisCacheExplorer = new RedisCacheExplorer(sid, id);
        return redisCacheExplorer;
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return TYPE;
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
