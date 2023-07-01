/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractAzureFacetNode<T> extends AbstractTreeNode<T> implements IAzureFacetNode {
    @Getter
    @Setter
    private boolean disposed;

    protected AbstractAzureFacetNode(Project project, @NotNull T value) {
        super(project, value);
    }

    @Override
    public @Nullable Object getData(@Nonnull String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, Action.SOURCE) ? this.getValue() : null;
    }

    public void dispose() {
        setDisposed(true);
    }

    public AbstractTreeNode<?> toExceptionNode(Throwable e) {
        e = ExceptionUtils.getRootCause(e);
        if (e instanceof AzureToolkitAuthenticationException) {
            final Action<Object> signin = AzureActionManager.getInstance().getAction(Action.AUTHENTICATE).bind(this.getProject()).withLabel("Sign in to manage connected resource");
            return new ActionNode<>(this.getProject(), signin);
        } else {
            return new ExceptionNode(this.getProject(), e);
        }
    }

    @ToString.Include
    @EqualsAndHashCode.Include
    public T getMyValue() {
        return this.getValue();
    }
}
