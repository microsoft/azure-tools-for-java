/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.microsoft.azure.toolkit.intellij.link.base.ResourceType;
import com.microsoft.azure.toolkit.intellij.link.po.BaseResourcePO;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AzureServiceStorage<T extends BaseResourcePO> {

    protected static final String ELEMENT_NAME_RESOURCES = "resources";
    protected static final String ELEMENT_NAME_RESOURCE = "resource";

    private Set<T> resources = new LinkedHashSet<>();

    public synchronized boolean addResource(T resource) {
        Iterator<T> iterator = resources.iterator();
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (StringUtils.equals(element.getBusinessUniqueKey(), resource.getBusinessUniqueKey())) {
                iterator.remove();
            }
        }
        return resources.add(resource);
    }

    public Set<T> getResources() {
        return this.resources;
    }

    public List<T> getResourcesByType(ResourceType type) {
        return resources.stream().filter(e -> Objects.equals(e.getType(), type)).collect(Collectors.toList());
    }

    public T getResourceById(String id) {
        return resources.stream().filter(e -> StringUtils.equals(e.getId(), id)).findFirst().orElse(null);
    }

    public T getResourceByBusinessUniqueKey(String businessUniqueKey) {
        return resources.stream().filter(e -> StringUtils.equals(e.getBusinessUniqueKey(), businessUniqueKey)).findFirst().orElse(null);
    }

}
