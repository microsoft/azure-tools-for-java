/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import org.jdom.Element;

import javax.annotation.Nonnull;

/**
 * the <b>{@code resource}</b> in <b>{@code resource connection}</b><br>
 * it's usually An Azure resource or an intellij module
 */
public interface Resource<T> {
    @Nonnull
    ResourceDefinition<T> getDefinition();

    @Nonnull
    default String getDefName() {
        return this.getDefinition().getName();
    }

    /**
     * get the id of the resource<br>
     * be careful <b>NOT</b> to return the Azure resource id directly since
     * this id will be saved somewhere in the workspace and may be tracked by git.<br>
     * a good practice would be returning the hashed(e.g. md5/sha1/sha256...) Azure resource id
     */
    T getData();

    String getId();

    String getName();

    default boolean writeTo(Element resourceEle) {
        return this.getDefinition().write(resourceEle, this);
    }
}
