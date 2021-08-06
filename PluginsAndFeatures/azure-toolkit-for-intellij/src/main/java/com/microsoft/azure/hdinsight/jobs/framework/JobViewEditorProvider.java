/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.jobs.framework;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/*
    All the tool window should implement interface FileEditorProvider.
 */
public class JobViewEditorProvider implements FileEditorProvider, DumbAware {

    public static Key<IClusterDetail> JOB_VIEW_KEY = new Key<>("com.microsoft.azure.hdinsight.jobview");
    public static Key<String> JOB_VIEW_UUID = new Key<>("com.microsoft.azure.hdinsight.jobview.uuid");

    private static Logger LOG = Logger.getInstance(JobViewEditorProvider.class.getName());

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        LOG.info("start JobViewEditorProvider");
        IClusterDetail detail = virtualFile.getUserData(JOB_VIEW_KEY);
        return detail != null;
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new JobViewEditor(project, virtualFile, this);
    }

    @Override
    public void disposeEditor(@NotNull FileEditor fileEditor) {
        Disposer.dispose(fileEditor);
    }

    @NotNull
    @Override
    public FileEditorState readState(@NotNull Element element, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void writeState(@NotNull FileEditorState fileEditorState, @NotNull Project project, @NotNull Element element) {

    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return this.getClass().getName();
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
