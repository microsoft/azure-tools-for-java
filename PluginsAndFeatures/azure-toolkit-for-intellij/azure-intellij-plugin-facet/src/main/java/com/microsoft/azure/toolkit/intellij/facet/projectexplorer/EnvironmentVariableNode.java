/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public class EnvironmentVariableNode extends AbstractAzureFacetNode<Pair<String, String>> implements Navigatable {
    private boolean visible;
    private final Connection<?, ?> connection;
    private final Action<?> editAction;
    private final Action<?> toggleVisibilityAction;

    public EnvironmentVariableNode(@Nonnull final Project project, Pair<String, String> generated, Connection<?, ?> connection) {
        super(project, generated);
        this.visible = false;
        this.connection = connection;
        this.editAction = new Action<>(Action.Id.of("user/connector.edit_env_in_editor"))
            .withLabel("Open In Editor")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .withHandler(ignore -> AzureTaskManager.getInstance().runLater(() -> this.navigate(true)))
            .withAuthRequired(false);
        this.toggleVisibilityAction = new Action<>(Action.Id.of("user/connector.toggle_env_visibility"))
            .withLabel(ignore -> this.visible ? "Hide Value" : "Show Value")
            .withHandler(ignore -> AzureTaskManager.getInstance().runLater(this::toggleVisibility))
            .withAuthRequired(false);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        final Pair<String, String> pair = this.getValue();
        presentation.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.VARIABLE));
        presentation.addText(pair.getKey(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        if (visible) {
            presentation.addText(" = " + pair.getValue(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        } else {
            presentation.addText(" = ***", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    @Nullable
    @Override
    public IActionGroup getActionGroup() {
        return new ActionGroup(
                editAction,
                "---",
                toggleVisibilityAction,
                ResourceConnectionActionsContributor.COPY_ENV_PAIR,
                ResourceConnectionActionsContributor.COPY_ENV_KEY
        );
    }

    private void toggleVisibility() {
        this.visible = !this.visible;
        this.rerender(false);
    }

    @Override
    public void navigate(boolean requestFocus) {
        final VirtualFile dovEnvFile = this.getDovEnvFile();
        final PsiFile psiFile = Optional.ofNullable(dovEnvFile)
                .map(f -> PsiManager.getInstance(getProject()).findFile(f)).orElse(null);
        if (Objects.isNull(psiFile)) {
            return;
        }
        NavigationUtil.openFileWithPsiElement(psiFile, requestFocus, requestFocus);
        EditorUtils.focusContentInCurrentEditor(getProject(), dovEnvFile, getValue().getKey() + "=");
    }

    @Override
    public boolean canNavigateToSource() {
        return Objects.nonNull(getDovEnvFile());
    }

    @Nullable
    private VirtualFile getDovEnvFile() {
        return Optional.ofNullable(connection).map(Connection::getProfile).map(Profile::getDotEnvFile).orElse(null);
    }

    @Override
    public boolean isAlwaysLeaf() {
        return true;
    }
}