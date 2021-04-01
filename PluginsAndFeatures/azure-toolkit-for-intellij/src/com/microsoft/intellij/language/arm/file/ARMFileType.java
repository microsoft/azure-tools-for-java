/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.language.arm.file;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.microsoft.intellij.language.arm.ARMLanguage;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ARMFileType extends LanguageFileType {

    public static final ARMFileType INSTANCE = new ARMFileType();
    public static final String DEFAULT_EXTENSION = "template";
    public static final String EXTENSIONS = "template";

    protected ARMFileType() {
        super(ARMLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "ARM_EX";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "ARM";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return EXTENSIONS;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return null;
    }
}
