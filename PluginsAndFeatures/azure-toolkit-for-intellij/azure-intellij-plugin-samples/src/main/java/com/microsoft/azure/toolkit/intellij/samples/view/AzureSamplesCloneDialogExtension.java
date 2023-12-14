/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.samples.view;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class AzureSamplesCloneDialogExtension implements VcsCloneDialogExtension {
    @Nonnull
    @Override
    public VcsCloneDialogExtensionComponent createMainComponent(@Nonnull final Project project, @Nonnull final ModalityState modalityState) {
        return new AzureSamplesCloneDialogExtensionComponent(project);
    }

    @Nonnull
    @Override
    public List<VcsCloneDialogExtensionStatusLine> getAdditionalStatusLines() {
        return Collections.singletonList(new VcsCloneDialogExtensionStatusLine("github.com/Azure-Samples", SimpleTextAttributes.GRAYED_ATTRIBUTES, e -> {
        }));
    }

    @Nonnull
    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE);
    }

    @Nls
    @Nonnull
    @Override
    public String getName() {
        return "Azure Samples";
    }

    @Nls
    @Nullable
    @Override
    public String getTooltip() {
        return "Microsoft Azure code samples and examples in Java";
    }
}
