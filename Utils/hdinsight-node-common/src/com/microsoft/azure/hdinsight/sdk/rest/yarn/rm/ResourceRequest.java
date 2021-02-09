/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.yarn.rm;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

public class ResourceRequest implements IConvertible {
    private String nodeLabelExpression;

    private Priority priority;

    private String relaxLocality;

    private String numContainers;

    private Capability capability;

    private String resourceName;

    public String getNodeLabelExpression() {
        return nodeLabelExpression;
    }

    public void setNodeLabelExpression(String nodeLabelExpression) {
        this.nodeLabelExpression = nodeLabelExpression;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getRelaxLocality() {
        return relaxLocality;
    }

    public void setRelaxLocality(String relaxLocality) {
        this.relaxLocality = relaxLocality;
    }

    public String getNumContainers() {
        return numContainers;
    }

    public void setNumContainers(String numContainers) {
        this.numContainers = numContainers;
    }

    public Capability getCapability() {
        return capability;
    }

    public void setCapability(Capability capability) {
        this.capability = capability;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
