/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

/**
 * the <b>{@code resource}</b> in <b>{@code resource connection}</b><br>
 * it's usually An Azure resource or an intellij module
 */
public interface Resource {
    String FIELD_TYPE = "type";
    String FIELD_BIZ_ID = "bizId";
    String FIELD_ID = "id";

    String getType();

    /**
     * get the id of the resource<br>
     * be careful <b>NOT</b> to return the Azure resource id directly since
     * this id will be saved somewhere in the workspace and may be tracked by git.<br>
     * a good practice would be returning the hashed(e.g. md5/sha1/sha256...) Azure resource id
     */
    String getId();
}
