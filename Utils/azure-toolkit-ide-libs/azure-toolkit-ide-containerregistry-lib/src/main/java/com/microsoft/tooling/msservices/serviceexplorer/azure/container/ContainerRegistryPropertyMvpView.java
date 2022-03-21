/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azuretools.core.mvp.ui.base.MvpView;
import com.microsoft.azuretools.core.mvp.ui.containerregistry.ContainerRegistryProperty;

import java.util.List;

public interface ContainerRegistryPropertyMvpView extends MvpView {

    void onReadProperty(String sid, String id);

    void showProperty(ContainerRegistryProperty property);

    void listRepo(List<String> repos);

    void listTag(List<String> tags);

}
