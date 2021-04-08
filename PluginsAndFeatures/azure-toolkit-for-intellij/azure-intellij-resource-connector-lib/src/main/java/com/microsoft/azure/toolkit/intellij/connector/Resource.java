/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

/**
 * the <b>{@code resource}</b> in <b>{@code resource connection}</b><br>
 * it's usually An Azure resource or an intellij module
 */
public interface Resource {
    String FIELD_TYPE = "type";
    String FIELD_BIZ_ID = "bizId";
    String FIELD_ID = "id";

    /**
     * get business id of the resource
     *
     * @return Azure resource id if this is an Azure resource or module name if this is a Intellij Module
     */
    @Nonnull
    String getBizId();

    String getType();

    default String getId() {
        return DigestUtils.md5Hex(this.getBizId());
    }
}
