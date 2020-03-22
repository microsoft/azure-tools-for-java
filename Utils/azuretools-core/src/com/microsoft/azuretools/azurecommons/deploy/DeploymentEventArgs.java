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
