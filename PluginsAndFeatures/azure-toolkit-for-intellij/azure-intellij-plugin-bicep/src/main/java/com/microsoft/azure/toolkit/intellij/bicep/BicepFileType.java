// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.microsoft.azure.toolkit.intellij.bicep;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class BicepFileType extends LanguageFileType {

    public static final BicepFileType INSTANCE = new BicepFileType();

    private BicepFileType() {
        super(BicepLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Bicep";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Bicep language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "bicep";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.Files.BICEP16);
    }
}
