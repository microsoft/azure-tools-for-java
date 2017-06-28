/**
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

package com.microsoft.azuretools.container.presenters;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.container.views.StepOnePageView;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ProgressMessage;
import com.spotify.docker.client.messages.RegistryAuth;

import rx.Observable;
import rx.schedulers.Schedulers;

import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.DockerRuntime;

public class StepOnePagePresenter<V extends StepOnePageView> extends MvpPresenter<V> {

    // StepOne Page
    public boolean onPushLatestImageToRegistry(String registryUrl, String registryUsername, String registryPassword) {
        try {
            getMvpView().showInfomation("Try pushing image ...");

            DockerClient dockerClient = DockerRuntime.getInstance().getDockerBuilder().build();
            ProgressHandler progressHandler = new ProgressHandler() {
                @Override
                public void progress(ProgressMessage message) throws DockerException {
                    if (message.error() != null) {
                        throw new DockerException(message.toString());
                    }
                }
            };

            // push image async
            Observable.fromCallable(() -> {
                doPushImage(dockerClient, registryUrl, registryUsername, registryPassword,
                        DockerRuntime.getInstance().getLatestImageName(), progressHandler);
                return null;
            }).subscribeOn(Schedulers.io()).subscribe(wal -> {
                // persist registry information
                DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                    doUpdateRuntimeRegistryInfo(registryUrl, registryUsername, registryPassword);
                    getMvpView().showInfomation("Task OK");
                    getMvpView().setWidgetsEnabledStatus(false);
                    getMvpView().setCompleteStatus(true);
                });
            }, e -> {
                DefaultLoader.getIdeHelper().invokeAndWait(() -> {
                    getMvpView().showInfomation("Task FAIL");
                    getMvpView().setWidgetsEnabledStatus(true);
                    getMvpView().setCompleteStatus(false);
                    ConsoleLogger.error("onPushLatestImageToRegistry@StepOnePagePresenter");
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            getMvpView().showInfomation(e.getMessage());
            return false;
        }
        return true;
    }

    public void onUpdateRegistryInfo(String registryUrl, String registryUsername, String registryPassword) {
        doUpdateRuntimeRegistryInfo(registryUrl, registryUsername, registryPassword);
    }

    public void onLoadRegistryInfo() {
        getMvpView().fillRegistryInfo(DockerRuntime.getInstance().getRegistryUrl(),
                DockerRuntime.getInstance().getRegistryUsername(), DockerRuntime.getInstance().getRegistryPassword());
    }

    // Private Helpers
    private void doUpdateRuntimeRegistryInfo(String registryUrl, String registryUsername, String registryPassword) {
        DockerRuntime.getInstance().setRegistryUrl(registryUrl);
        DockerRuntime.getInstance().setRegistryUsername(registryUsername);
        DockerRuntime.getInstance().setRegistryPassword(registryPassword);
    }

    private void doPushImage(DockerClient dockerClient, String registryUrl, String registryUsername,
            String registryPassword, String latestImageName, ProgressHandler handler)
            throws DockerException, InterruptedException {
        final String targetName = String.format("%s/%s", registryUrl, latestImageName);
        final RegistryAuth registryAuth = RegistryAuth.builder().username(registryUsername).password(registryPassword)
                .build();
        dockerClient.tag(latestImageName, targetName);
        dockerClient.push(targetName, handler, registryAuth);
    }
}
