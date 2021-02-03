/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.deploy;

import java.util.EventObject;

public class UploadProgressEventArgs extends EventObject {

    private static final long serialVersionUID = -7144157071013651398L;

    private int percentage;

    public UploadProgressEventArgs(Object source) {
        super(source);

    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }
}
