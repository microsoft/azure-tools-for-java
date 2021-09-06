/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.azureexplorer.helpers;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.eclipse.ui.PlatformUI;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;

public class IDEHelperImpl implements IDEHelper {
    public static final String DEFAULT_PROJECT = "DEFAULT_PROJECT";

    @Override
    public void closeFile(Object projectObject, Object openedFile) {
    }

    @Override
    public void invokeLater(Runnable runnable) {
        AzureTaskManager.getInstance().runLater(runnable);
    }

    @Override
    public void invokeAndWait(Runnable runnable) {
        AzureTaskManager.getInstance().runAndWait(runnable);
    }

    @Override
    public void executeOnPooledThread(final Runnable runnable) {
        AzureTaskManager.getInstance().runOnPooledThread(runnable);
    }

    @Override
    public String getProperty(String name, Object projectObject) {
        return getProperty(name);
    }

    public String getProperty(Object projectObject, String name, String defaultValue) {
        return null;
    }

    @Override
    public void setProperty(String name, String value, Object projectObject) {
        setProperty(name, value);
    }

    @Override
    public void unsetProperty(String name, Object projectObject) {
        unsetProperty(name);
    }

    public boolean isPropertySet(Object projectObject, String name) {
        return false;
    }

    @Override
    public String getProperty(String name) {
        return PreferenceUtil.loadPreference(name);
    }

    @Override
    public String getPropertyWithDefault(String name, String defaultValue) {
        return PreferenceUtil.loadPreference(name, defaultValue);
    }

    @Override
    public void setProperty(String name, String value) {
        PreferenceUtil.savePreference(name, value);
    }

    @Override
    public void unsetProperty(String name) {
        PreferenceUtil.unsetPreference(name);
    }

    @Override
    public boolean isPropertySet(String name) {
        return getProperty(name) != null;
    }

    @Override
    public String[] getProperties(String name) {
        return PreferenceUtil.loadPreferences(name);
    }

    @Override
    public String[] getProperties(String name, Object project) {
        return getProperties(name);
    }

    @Override
    public void setProperties(String name, String[] value) {
        PreferenceUtil.savePreferences(name, value);
    }

    @Override
    public List<ArtifactDescriptor> getArtifacts(
            ProjectDescriptor projectDescriptor) throws AzureCmdException {
        return null;
    }

    @Override
    public ListenableFuture<String> buildArtifact(
            ProjectDescriptor projectDescriptor,
            ArtifactDescriptor artifactDescriptor) {
        return null;
    }

    public Object getCurrentProject() {
        return DEFAULT_PROJECT;
    }

    @Override
    public void setApplicationProperty(@NotNull String name, @NotNull String value) {
        setProperty(name, value);
    }

    @Override
    public void unsetApplicationProperty(@NotNull String name) {
        unsetProperty(name);
    }

    @Override
    @Nullable
    public String getApplicationProperty(@NotNull String name) {
        return getProperty(name);
    }

    @Override
    public void setApplicationProperties(@NotNull String name, @NotNull String[] value) {
        setProperties(name, value);
    }

    @Override
    public void unsetApplicatonProperties(@NotNull String name) {
        unsetProperty(name);
    }

    @Override
    @Nullable
    public String[] getApplicationProperties(@NotNull String name) {
        return getProperties(name);
    }

    @Override
    public boolean isApplicationPropertySet(@NotNull String name) {
        return isPropertySet(name);
    }

    @Override
    public String getProjectSettingsPath() {
        return String.format("%s%s%s", PluginUtil.pluginFolder, File.separator, Messages.commonPluginID);
    }

    @Override
    public void openLinkInBrowser(@NotNull String url) {
        try {
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().showException("Unexpected exception: " + ex.getMessage(), ex, "Browse Web App", true, false);
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
    }
}
