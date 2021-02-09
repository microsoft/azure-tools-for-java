/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.deploy;

import com.microsoft.azuretools.azurecommons.messagehandler.PropUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;
import java.util.Locale;

public class DeploymentEventArgs extends EventObject {

    String deploymentCanceled = PropUtil.getValueFromFile("deploymentCanceled");
    String dateFormatEventArgs = PropUtil.getValueFromFile("dateFormatEventArgs");
    String toStringFormat = PropUtil.getValueFromFile("toStringFormat");

    private static final long serialVersionUID = 1757673237718513593L;

    private String id;
    private String deployMessage;
    private int deployCompleteness;
    private Date startTime;
    private String deploymentURL;

    public String getDeploymentURL() {
        return deploymentURL;
    }

    public void setDeploymentURL(String deploymentURL) {
        this.deploymentURL = deploymentURL;
    }

    public DeploymentEventArgs(Object source) {
        super(source);
    }

    public String getDeployMessage() {
        return deployMessage;
    }

    public void setDeployMessage(String deployMessage) {
        this.deployMessage = deployMessage;
    }

    public int getDeployCompleteness() {
        return deployCompleteness;
    }

    public void setDeployCompleteness(int deployCompleteness) {
        this.deployCompleteness = deployCompleteness;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        if (deployMessage.equals(deploymentCanceled)) {
            return deployMessage;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                dateFormatEventArgs, Locale.getDefault());
        String format = String.format(toStringFormat, dateFormat.format(getStartTime()), getDeployMessage());
        return format;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
