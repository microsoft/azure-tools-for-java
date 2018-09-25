package com.microsoft.intellij.docker.wizards.publish.forms;

import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.management.Azure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;

public class CalculateDockerDeletionSafenessBackgroundTask extends Task.Backgroundable{

  private Boolean result;
  private AsyncPromise<Boolean> promise;
  private Azure azureClient;
  private DockerHost dockerHost;

  public CalculateDockerDeletionSafenessBackgroundTask(
          AsyncPromise<Boolean> isDeletingDockerHostSafe,
          Azure azureClient,
          DockerHost dockerHost) {
    super(null, "Checking Docker Host", true, PerformInBackgroundOption.DEAF);
    this.promise = isDeletingDockerHostSafe;
    this.azureClient = azureClient;
    this.dockerHost = dockerHost;
  }

  @Override
  public void run(@NotNull ProgressIndicator progressIndicator) {
    result = AzureDockerVMOps.isDeletingDockerHostAllSafe(
            azureClient,
            dockerHost.hostVM.resourceGroupName,
            dockerHost.hostVM.name);
  }

  @Override
  public void onCancel() {
    promise.cancel();
  }

  @Override
  public void onSuccess() {
    promise.setResult(result);
  }

  @Override
  public void onThrowable(@NotNull Throwable error) {
    promise.setError(error);
  }
}
