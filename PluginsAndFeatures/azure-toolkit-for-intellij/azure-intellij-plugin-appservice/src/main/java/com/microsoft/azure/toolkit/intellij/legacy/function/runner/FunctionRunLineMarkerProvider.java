/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FunctionRunLineMarkerProvider extends RunLineMarkerContributor {
    @Nullable
    @Override
    public Info getInfo(@NotNull PsiElement e) {
        if (isIdentifier(e)) {
            final PsiElement parentElement = e.getParent();
            if (parentElement instanceof PsiMethod && FunctionUtils.isFunctionClassAnnotated((PsiMethod) parentElement)) {
                final AnAction[] actions = ExecutorAction.getActions(1);
                return new Info(AllIcons.RunConfigurations.TestState.Run, actions,
                    element -> StringUtil.join(ContainerUtil.mapNotNull(actions, action -> getText(action, element)), "\n"));
            }
        }
        return null;
    }

    private static boolean isIdentifier(PsiElement e) {
        return e instanceof PsiIdentifier;
    }
}
