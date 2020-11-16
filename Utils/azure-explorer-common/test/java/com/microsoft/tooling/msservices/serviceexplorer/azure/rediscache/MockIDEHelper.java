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

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.IDEHelper;

public class MockIDEHelper implements IDEHelper{
    public static final String DEFAULT_PROJECT = "DEFAULT_PROJECT";

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
