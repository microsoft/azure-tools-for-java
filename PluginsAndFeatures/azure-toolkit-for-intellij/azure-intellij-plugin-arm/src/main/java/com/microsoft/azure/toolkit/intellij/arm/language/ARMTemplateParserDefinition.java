/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.language;

import com.intellij.json.JsonLexer;
import com.intellij.json.JsonParserDefinition;
import com.intellij.json.psi.impl.JsonFileImpl;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;


public class ARMTemplateParserDefinition extends JsonParserDefinition {

    static final IFileElementType ARMFILE = new IFileElementType(ARMTemplateLanguage.INSTANCE);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new MergingLexerAdapter(new JsonLexer(), TokenSet.ANY);
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new JsonFileImpl(fileViewProvider, ARMTemplateLanguage.INSTANCE);
    }

    @Override
    public IFileElementType getFileNodeType() {
        return ARMFILE;
    }

}
