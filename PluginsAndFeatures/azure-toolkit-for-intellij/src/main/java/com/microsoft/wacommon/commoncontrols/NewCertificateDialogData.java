/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.wacommon.commoncontrols;

/**
 * This class is useful to pass and return values from dialog
 */
public class NewCertificateDialogData {

    private String cerFilePath;
    private String pfxFilePath;
    private String password;
    private String cnName;

    public String getCerFilePath() {
        return cerFilePath;
    }

    public void setCerFilePath(String cerFilePath) {
        this.cerFilePath = cerFilePath;
    }

    public String getPfxFilePath() {
        return pfxFilePath;
    }

    public void setPfxFilePath(String pfxFilePath) {
        this.pfxFilePath = pfxFilePath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCnName() {
        return cnName;
    }

    public void setCnName(String cnName) {
        this.cnName = cnName;
    }
}
