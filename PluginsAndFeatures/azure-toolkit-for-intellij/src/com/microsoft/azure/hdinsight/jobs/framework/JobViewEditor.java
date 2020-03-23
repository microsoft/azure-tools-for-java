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

package com.microsoft.azure.hdinsight.jobs.framework;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.spark.jobs.framework.JobViewPanel;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class JobViewEditor implements FileEditor {

    protected final Project myProject;
    private final JobViewEditorProvider myProvider;
    private final VirtualFile myVirtualFile;
    @NotNull
    private final JComponent myComponent;

    @NotNull
    private final String uuid;

    private static Logger LOG = Logger.getInstance(JobViewEditor.class.getName());

    public JobViewEditor(@NotNull final Project project, @NotNull final VirtualFile file, final JobViewEditorProvider provider) {
        LOG.info("start open JobView page");
        myProject = project;
        myProvider = provider;
        myVirtualFile = file;
        uuid = file.getUserData(JobViewEditorProvider.JOB_VIEW_UUID);
        myComponent = new JobViewPanel(PluginUtil.getPluginRootDirectory(), uuid);
        AppInsightsClient.create(HDInsightBundle.message("HDInsightSparkJobview"), null);
        EventUtil.logEvent(EventType.info, TelemetryConstants.HDINSIGHT,
            HDInsightBundle.message("HDInsightSparkJobview"), null);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return myComponent;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myComponent;
    }

    @NotNull
    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel fileEditorStateLevel) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {

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
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {

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
    public <T> T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {

    }
}
