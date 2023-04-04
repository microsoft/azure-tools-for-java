/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.jobs.framework;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import lombok.CustomLog;
import org.jdom.Element;

import javax.annotation.Nonnull;

/*
    All the tool window should implement interface FileEditorProvider.
 */
@CustomLog
public class JobViewEditorProvider implements FileEditorProvider, DumbAware {

    public static Key<IClusterDetail> JOB_VIEW_KEY = new Key<>("com.microsoft.azure.hdinsight.jobview");
    public static Key<String> JOB_VIEW_UUID = new Key<>("com.microsoft.azure.hdinsight.jobview.uuid");

    @Override
    public boolean accept(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        log.info("start JobViewEditorProvider");
        IClusterDetail detail = virtualFile.getUserData(JOB_VIEW_KEY);
        return detail != null;
    }

    @Nonnull
    @Override
    public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        return new JobViewEditor(project, virtualFile, this);
    }

    @Override
    public void disposeEditor(@Nonnull FileEditor fileEditor) {
        Disposer.dispose(fileEditor);
    }

    @Nonnull
    @Override
    public FileEditorState readState(@Nonnull Element element, @Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void writeState(@Nonnull FileEditorState fileEditorState, @Nonnull Project project, @Nonnull Element element) {

    }

    @Nonnull
    @Override
    public String getEditorTypeId() {
        return this.getClass().getName();
    }

    @Nonnull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
