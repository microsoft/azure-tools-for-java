/**
 * Copyright (c) 2018 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.docker.wizards.publish.forms;

import com.intellij.openapi.progress.PerformInBackgroundOption;
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
