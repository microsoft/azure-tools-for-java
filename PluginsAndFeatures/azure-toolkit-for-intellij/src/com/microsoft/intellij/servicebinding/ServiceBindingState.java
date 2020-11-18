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

package com.microsoft.intellij.servicebinding;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@State(name = "binding-service-config", storages = {@Storage("azure-service-binding-idea.xml")})
public class ServiceBindingState implements PersistentStateComponent<ServiceBindingState> {
    @XCollection
    private List<InternalBindingInfo> serviceBindings = new ArrayList<>();

    @Nullable
    @Override
    public ServiceBindingState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ServiceBindingState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public void addBindingInfo(@NotNull ServiceBindingInfo info) {
        ServiceBindingState newServiceBindings = new ServiceBindingState();
        newServiceBindings.serviceBindings.addAll(this.serviceBindings);
        newServiceBindings.serviceBindings.add(InternalBindingInfo.fromBindingInfo(info));
        loadState(newServiceBindings);
    }

    public List<ServiceBindingInfo> getServiceBindingInfos() {
        if (CollectionUtils.isEmpty(serviceBindings)) {
            return Collections.emptyList();
        }
        return serviceBindings.stream().map(InternalBindingInfo::toBindingInfo).collect(Collectors.toList());
    }
}
