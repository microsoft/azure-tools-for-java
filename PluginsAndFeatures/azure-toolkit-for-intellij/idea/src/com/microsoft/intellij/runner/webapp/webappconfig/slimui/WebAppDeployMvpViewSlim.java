package com.microsoft.intellij.runner.webapp.webappconfig.slimui;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

import java.util.List;

public interface WebAppDeployMvpViewSlim extends MvpView {
    void fillWebApps(@NotNull List<ResourceEx<WebApp>> webAppLists);
    void fillDeploymentSlots(@NotNull List<DeploymentSlot> slots);
}
