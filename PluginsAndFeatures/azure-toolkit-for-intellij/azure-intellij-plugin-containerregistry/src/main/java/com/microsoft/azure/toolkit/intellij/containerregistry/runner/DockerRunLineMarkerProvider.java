/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.containerregistry.runner;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtil;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.action.PushImageAction;
import com.microsoft.azure.toolkit.intellij.containerregistry.action.RunOnDockerHostAction;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;

public class DockerRunLineMarkerProvider implements LineMarkerProvider {
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psi) {
        final ASTNode node = psi.getNode();
        if (node == null) {
            return null;
        }
        final VirtualFile virtualFile = PsiUtil.getVirtualFile(psi);
        final String fileName = virtualFile != null ? virtualFile.getName() : null;
        return (StringUtils.equals(fileName, "Dockerfile") && isFromCommand(psi) && findPreviousFromCommand(psi) == null) ? getDockerRunLineMarkerInfo(psi) : null;
    }

    private static boolean isFromCommand(@Nonnull PsiElement psiElement) {
        final boolean startWithFrom = StringUtils.startsWith(psiElement.getText(), "FROM");
        final boolean hasChildCommand = Arrays.stream(psiElement.getChildren()).anyMatch(child -> StringUtils.startsWith(psiElement.getText(), "FROM"));
        return startWithFrom && !hasChildCommand;
    }

    private static @Nullable PsiElement findPreviousFromCommand(@NotNull PsiElement psiElement) {
        for (PsiElement cur = psiElement.getPrevSibling(); cur != null; cur = cur.getPrevSibling()) {
            if (isFromCommand(cur)) {
                return cur;
            }
        }
        return null;
    }

    private LineMarkerInfo<?> getDockerRunLineMarkerInfo(PsiElement psi) {
        final VirtualFile file = PsiUtil.getVirtualFile(psi);
        final DockerImage dockerImage = new DockerImage(file);
        final ActionGroup actionGroup = new ActionGroup() {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                final RunOnDockerHostAction runAction = new RunOnDockerHostAction(dockerImage);
                final PushImageAction pushAction = new PushImageAction(dockerImage);
                return new AnAction[]{runAction, pushAction};
            }
        };
        final Icon icon = IntelliJAzureIcons.getIcon(AzureIcons.ContainerRegistry.MODULE);
        return new LineMarkerInfo(psi, psi.getTextRange(), icon, ignore -> "Azure Docker Actions", null, GutterIconRenderer.Alignment.CENTER) {
            @Override
            public GutterIconRenderer createGutterRenderer() {
                return new LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>(this) {
                    @Override
                    public AnAction getClickAction() {
                        return null;
                    }

                    @Override
                    public boolean isNavigateAction() {
                        return true;
                    }

                    @Override
                    public @Nullable ActionGroup getPopupMenuActions() {
                        return actionGroup;
                    }
                };
            }
        };
    }

}
