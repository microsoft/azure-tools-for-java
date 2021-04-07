/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public interface Resource {
    String FIELD_TYPE = "type";
    String FIELD_BIZ_ID = "bizId";
    String FIELD_ID = "id";

    @Nonnull
    String getBizId();

    default void setType(String type) {
        assert StringUtils.equals(getDefinition().getType(), type) : String.format("incompatible resource type \"%s\":\"%s\"", getDefinition().getType(), type);
    }

    default String getId() {
        return DigestUtils.md5Hex(this.getBizId());
    }

    ResourceDefinition<? extends Resource> getDefinition();
}
