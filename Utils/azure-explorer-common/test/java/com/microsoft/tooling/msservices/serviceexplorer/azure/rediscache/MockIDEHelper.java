package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.tasks.CancellableTask;
import com.microsoft.azuretools.azurecommons.tasks.CancellableTask.CancellableTaskHandle;
import com.microsoft.tooling.msservices.helpers.IDEHelper;

public class MockIDEHelper implements IDEHelper{
    public static final String DEFAULT_PROJECT = "DEFAULT_PROJECT";
    @Override
    public void runInBackground(Object project, String name, boolean canBeCancelled, boolean isIndeterminate, final String indicatorText, final Runnable runnable) {
    }

    @Override
    public void closeFile(Object projectObject, Object openedFile) {

    }

    @Override
    public void invokeLater(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void invokeAndWait(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void executeOnPooledThread(final Runnable runnable) {

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
        return "";
    }

    @Override
    public String getPropertyWithDefault(String name, String defaultValue) {
        return "";
    }

    @Override
    public void setProperty(String name, String value) {}

    @Override
    public void unsetProperty(String name) {
    }

    @Override
    public boolean isPropertySet(String name) {
        return false;
    }

    @Override
    public String[] getProperties(String name) {
        return null;
    }

    @Override
    public String[] getProperties(String name, Object project) {
        return null;
    }

    @Override
    public void setProperties(String name, String[] value) {
    }

    @Override
    public CancellableTaskHandle runInBackground(
            ProjectDescriptor projectDescriptor, String name,
            String indicatorText, CancellableTask cancellableTask)
            throws AzureCmdException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ArtifactDescriptor> getArtifacts(
            ProjectDescriptor projectDescriptor) throws AzureCmdException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<String> buildArtifact(
            ProjectDescriptor projectDescriptor,
            ArtifactDescriptor artifactDescriptor) {
        // TODO Auto-generated method stub
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
        return "";
    }
    
    @Override
    public void openLinkInBrowser(@NotNull String url) {

    }
}
