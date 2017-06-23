package com.microsoft.azuretools.container.presenters;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.container.ui.wizard.publish.StepOnePage;
import com.microsoft.azuretools.core.components.AzureWizardPage;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ProgressMessage;
import com.spotify.docker.client.messages.RegistryAuth;
import com.microsoft.azuretools.container.DockerRuntime;

public class PublishWizardPresenter<V extends AzureWizardPage> extends MvpPresenter<V> {

    // StepOne Page
    public boolean onAckRegistry(String registryUrl, String registryUsername, String registryPassword) {
        StepOnePage s1 = (StepOnePage) this.getMvpView();
        try {
            s1.showInfomation("onAckRegistry");
            DockerClient dockerClient = DockerRuntime.getInstance().getDockerBuilder().build();
            ProgressHandler progressHandler = new ProgressHandler() {
                @Override
                public void progress(ProgressMessage message) throws DockerException {
                    if (message.error() != null) {
                        throw new DockerException(message.toString());
                    }
                    s1.showInfomation(message.toString());
                }
            };

            // TODO: push image
            doPushImage(dockerClient, registryUrl, registryUsername, registryPassword,
                    DockerRuntime.getInstance().getLatestImageName(), progressHandler);
            // TODO: persist registry information
            doUpdateRegistryInfo(registryUrl, registryUsername, registryPassword);

        } catch (Exception e) {
            s1.showInfomation(e.getMessage());
            return false;
        }
        return true;
    }

    public void onUpdateRegistryInfo(String registryUrl, String registryUsername, String registryPassword) {
        doUpdateRegistryInfo(registryUrl, registryUsername, registryPassword);
    }

    public void onLoadRegistryInfo() {
        StepOnePage s1 = (StepOnePage) this.getMvpView();
        s1.fillRegistryInfo(DockerRuntime.getInstance().getRegistryUrl(),
                DockerRuntime.getInstance().getRegistryUsername(), DockerRuntime.getInstance().getRegistryPassword());
    }

    // StepTwo Page

    // Private Helpers
    private void doUpdateRegistryInfo(String registryUrl, String registryUsername, String registryPassword) {
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
