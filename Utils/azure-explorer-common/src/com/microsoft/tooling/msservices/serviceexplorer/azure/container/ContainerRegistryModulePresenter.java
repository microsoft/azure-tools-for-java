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

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azure.management.containerregistry.Registries;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ContainerRegistryModulePresenter<V extends ContainerRegistryModule> extends MvpPresenter<V> {

    private final ContainerRegistryMvpModel containerRegistryMvpModel = ContainerRegistryMvpModel.getInstance();

    /**
     * Called from view when the view needs refresh.
     */
    public void onModuleRefresh() {
        HashMap<String, ArrayList<NodeContent>> nodeMap = new HashMap<>();
        try {
            Map<String, Registries> registriesMap = containerRegistryMvpModel.getContainerRegistries();
            for (String sid : registriesMap.keySet()) {
                ArrayList<NodeContent> nodeContentList = new ArrayList<>();
                for (Registry registry : registriesMap.get(sid).list()) {
                    nodeContentList
                            .add(new NodeContent(registry.id(), registry.name(), "" /*provisionState*/));
                }
                nodeMap.put(sid, nodeContentList);
            }
        } catch (Exception e) {
            getMvpView().onErrorWithException(e.getMessage(), e);
            return;
        }
        getMvpView().showNode(nodeMap);
    }
}
