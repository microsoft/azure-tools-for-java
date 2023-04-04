/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.jobs.framework;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.intellij.util.PluginUtil;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.WeakHashMap;

@CustomLog
public class JobViewEditor implements FileEditor {

    protected final Project myProject;
    private final JobViewEditorProvider myProvider;
    private final VirtualFile myVirtualFile;
    @Nonnull
    private final JobViewPanel jobViewPanel;

    @Nonnull
    private final String uuid;

    private WeakHashMap<Key,Object> userDatas = new WeakHashMap<Key,Object>();

    public JobViewEditor(@Nonnull final Project project, @Nonnull final VirtualFile file, final JobViewEditorProvider provider) {
        log.info("start open JobView page");
        myProject = project;
        myProvider = provider;
        myVirtualFile = file;
        uuid = file.getUserData(JobViewEditorProvider.JOB_VIEW_UUID);
        jobViewPanel = new JobViewPanel(PluginUtil.getPluginRootDirectory() + "/com.microsoft.hdinsight", uuid);
        AppInsightsClient.create(HDInsightBundle.message("HDInsightSparkJobview"), null);
        EventUtil.logEvent(EventType.info, TelemetryConstants.HDINSIGHT,
            HDInsightBundle.message("HDInsightSparkJobview"), null);
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return jobViewPanel.getComponent();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return jobViewPanel.getBrowser().getComponent();
    }

    @Nonnull
    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Nonnull
    @Override
    public FileEditorState getState(@Nonnull FileEditorStateLevel fileEditorStateLevel) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@Nonnull FileEditorState fileEditorState) {

    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {

    }

    @Override
    public void deselectNotify() {

    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener propertyChangeListener) {

    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener propertyChangeListener) {

    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @Override
    public void dispose() {
        AppInsightsClient.create(HDInsightBundle.message("HDInsightSparkJobView.Close"), null);
        EventUtil.logEvent(EventType.info, TelemetryConstants.HDINSIGHT,
            HDInsightBundle.message("HDInsightSparkJobView.Close"), null);
    }

    @Nullable
    @Override
    public <T> T getUserData(@Nonnull Key<T> key) {
        if (userDatas.containsKey(key)) {
            WeakHashMap<Key<T>,Object> userData = new WeakHashMap<Key<T>,Object>();
            userData.put(key,userDatas.get(key));
            return (T) userData;
        }
        return null;
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, @Nullable T t) {
        this.userDatas.put(key, t);
    }

    @Override
    public @org.jetbrains.annotations.Nullable VirtualFile getFile() {
        return myVirtualFile;
    }
}
