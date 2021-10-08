/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.property;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.redis.RedisCache;

import javax.annotation.Nonnull;

public class RedisCachePropertiesEditorProvider implements FileEditorProvider, DumbAware {

    public static final String TYPE = "REDISCACHE_FILE_TYPE";

    @Override
    public boolean accept(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(getEditorTypeId());
    }

    @Nonnull
    @Override
    public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        final RedisCache redis = (RedisCache) virtualFile.getUserData(IntellijShowPropertiesViewAction.AZURE_RESOURCE_KEY);
        assert redis != null;
        return new RedisCachePropertiesEditor(project, redis, virtualFile);
    }

    @Nonnull
    @Override
    public String getEditorTypeId() {
        return TYPE;
    }

    @Nonnull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
