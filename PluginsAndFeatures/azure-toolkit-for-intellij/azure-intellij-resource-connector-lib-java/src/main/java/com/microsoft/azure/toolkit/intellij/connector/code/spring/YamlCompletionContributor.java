package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

public class YamlCompletionContributor extends CompletionContributor {

    public static final ElementPattern<? extends PsiFile> APPLICATION_YAML_FILE = PlatformPatterns.psiFile(YAMLFile.class).withName(StandardPatterns.string().startsWith("application"));
    public static final PsiElementPattern.Capture<PsiElement> YAML_TEXT = PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).inFile(APPLICATION_YAML_FILE);

    public YamlCompletionContributor() {
        super();
        final YamlKeyCompletionProvider keyCompletionProvider = new YamlKeyCompletionProvider();
        extend(CompletionType.BASIC, YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLDocument.class)), keyCompletionProvider);
        extend(CompletionType.BASIC, YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLMapping.class)), keyCompletionProvider);
        extend(CompletionType.BASIC, YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLKeyValue.class)), keyCompletionProvider);
        final YamlValueCompletionProvider valueCompletionProvider = new YamlValueCompletionProvider();
        extend(CompletionType.BASIC, YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLKeyValue.class)), valueCompletionProvider);
    }
}
