/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.apache.commons.codec.digest.DigestUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

/**
 * the <b>{@code resource}</b> in <b>{@code resource connection}</b><br>
 * it's usually An Azure resource or an intellij module
 */
public interface Resource<T> {
    @Nonnull
    ResourceDefinition<T> getDefinition();

    /**
     * get the id of the resource<br>
     * be careful <b>NOT</b> to return the Azure resource id directly since
     * this id will be saved somewhere in the workspace and may be tracked by git.<br>
     * a good practice would be returning the hashed(e.g. md5/sha1/sha256...) Azure resource id
     */
    String getId();

    T getData();

    String getDataId();

    String getName();

    default boolean writeTo(Element resourceEle) {
        return this.getDefinition().write(resourceEle, this);
    }

    default void navigate(AnActionEvent event) {
    }

    default Map<String, String> initEnv(Project project) {
        return Collections.emptyMap();
    }

    default boolean isModified(Resource<T> resource) {
        return !this.equals(resource);
    }

    default boolean isValidResource() {
        return true;
    }

    default boolean canConnectSilently() {
        return true;
    }
}
