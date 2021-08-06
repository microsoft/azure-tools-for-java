/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.language.arm;

import com.intellij.lang.Language;

public class ARMLanguage extends Language {

    public static final ARMLanguage INSTANCE = new ARMLanguage();
    public static final String MIME_TYPE = "application/x-template";
    public static final String MIME_TYPE2 = "application/template";
    public static final String ID = "arm";

    public ARMLanguage() {
        super(ID, MIME_TYPE, MIME_TYPE2);
    }

}
