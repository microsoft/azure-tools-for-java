/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
