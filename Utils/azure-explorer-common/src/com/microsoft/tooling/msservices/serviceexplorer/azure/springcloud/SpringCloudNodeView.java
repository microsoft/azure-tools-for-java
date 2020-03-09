package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentResource;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

import java.util.List;
import java.util.Map;

public interface SpringCloudNodeView extends MvpView {
    void renderSpringCloudApps(List<AppResourceInner> apps, Map<String, DeploymentResource> map);
}
