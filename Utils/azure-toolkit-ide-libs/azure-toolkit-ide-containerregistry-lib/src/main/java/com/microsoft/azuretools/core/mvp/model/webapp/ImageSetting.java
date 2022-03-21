/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

public abstract class ImageSetting {
    private String imageNameWithTag;
    private String startupFile;

    public ImageSetting() {
    }

    public ImageSetting(String imageNameWithTag, String startupFile) {
        this.imageNameWithTag = imageNameWithTag;
        this.startupFile = startupFile;
    }

    public String getImageNameWithTag() {
        return imageNameWithTag;
    }

    public void setImageNameWithTag(String imageNameWithTag) {
        this.imageNameWithTag = imageNameWithTag;
    }

    public String getStartupFile() {
        return startupFile;
    }

    public void setStartupFile(String startupFile) {
        this.startupFile = startupFile;
    }
}
