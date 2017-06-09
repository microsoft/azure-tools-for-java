package com.microsoft.intellij.helpers.rediscache;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.helpers.UIHelperImpl;
import org.jetbrains.annotations.NotNull;

public class RedisCachePropertyViewProvider implements FileEditorProvider {

    private static final String REDIS_PROPERTY_TYPE = "Microsoft.Cache/Redis";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(REDIS_PROPERTY_TYPE);
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        RedisCachePropertyView redisCachePropertyView = new RedisCachePropertyView();
        return redisCachePropertyView;
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return REDIS_PROPERTY_TYPE;
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
