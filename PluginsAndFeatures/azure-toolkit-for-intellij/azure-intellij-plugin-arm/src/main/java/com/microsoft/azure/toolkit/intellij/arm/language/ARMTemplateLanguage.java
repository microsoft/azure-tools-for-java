/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.language;

import com.intellij.lang.Language;

public class ARMTemplateLanguage extends Language {

    public static final ARMTemplateLanguage INSTANCE = new ARMTemplateLanguage();
    public static final String MIME_TYPE = "application/x-template";
    public static final String MIME_TYPE2 = "application/template";
    public static final String ID = "arm";

    public ARMTemplateLanguage() {
        super(ID, MIME_TYPE, MIME_TYPE2);
    }

}
