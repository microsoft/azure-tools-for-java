/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.code.spring;

import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.AnnotationFixes;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.projectexplorer.AbstractAzureFacetNode;
import com.microsoft.azure.toolkit.intellij.keyvault.connection.KeyVaultResourceDefinition;
import com.microsoft.azure.toolkit.intellij.keyvault.creation.secret.SecretCreationActions;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.secret.SecretDraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLPsiElement;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.keyvault.code.spring.EnvVarCompletionContributor.KEYVAULT_SECRET_ENV_VAR_PLACES;

public class PlainTextSecretAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (KEYVAULT_SECRET_ENV_VAR_PLACES.accepts(element)) {
            final String key = element.getContainingFile() instanceof PropertiesFileImpl ?
                Optional.of(element).map(PsiElement::getParent).map(PsiElement::getFirstChild)
                    .map(PsiElement::getText).orElse("") :
                Optional.of(element).map(e -> PsiTreeUtil.getParentOfType(e, YAMLPsiElement.class))
                    .map(YAMLUtil::getConfigFullName).orElse("");
            final String value = element.getText();
            final SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(element);
            if (EnvVarCompletionContributor.isSecretKey(key) && !EnvVarCompletionContributor.hasEnvVars(value)) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Secret is in plain text.")
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    .withFix(new IntentionAndQuickFixAction() {
                        @Override
                        public @IntentionName @NotNull String getName() {
                            //noinspection DialogTitleCapitalization
                            return "Create Secret in Azure Key Vault";
                        }

                        @Override
                        public @IntentionFamilyName @NotNull String getFamilyName() {
                            return "Azure general fixes";
                        }

                        @Override
                        @AzureOperation("user/connector.create_keyvault_secrets_from_plain_text")
                        public void applyFix(@NotNull final Project project, final PsiFile file, @Nullable final Editor editor) {
                            if (Objects.isNull(editor)) {
                                return;
                            }
                            final AzureTaskManager tm = AzureTaskManager.getInstance();
                            tm.runOnPooledThread(() -> {
                                final Optional<Connection<KeyVault, ?>> optConn = Optional.ofNullable(ModuleUtil.findModuleForFile(file)).map(AzureModule::from).stream()
                                    .flatMap(m -> m.getConnections(KeyVaultResourceDefinition.INSTANCE).stream()).filter(Connection::isValidConnection).findAny();
                                if (optConn.isEmpty()) { // has no Azure KeyVault connection.
                                    tm.runLater(() -> AnnotationFixes.createNewConnection(KeyVaultResourceDefinition.INSTANCE, (c) -> {
                                        final KeyVault vault = (KeyVault) c.getResource().getData();
                                        createSecretAndReplace(c, project, editor, vault);
                                    }).invoke(project, editor, file));
                                } else {
                                    final KeyVault vault = optConn.get().getResource().getData();
                                    createSecretAndReplace(optConn.get(), project, editor, vault);
                                }
                            });
                        }

                        private void createSecretAndReplace(final Connection<?, ?> connection, @Nonnull Project project, Editor editor, KeyVault vault) {
                            final SecretDraft.Config config = new SecretDraft.Config();
                            config.setValue(value);
                            SecretCreationActions.createNewSecret(vault, config, s -> WriteCommandAction.runWriteCommandAction(project, () ->
                                Optional.ofNullable(pointer.getElement()).map(PsiElement::getTextRange).ifPresent(range -> {
                                    final Document document = editor.getDocument();
                                    document.replaceString(range.getStartOffset(), range.getEndOffset(), String.format("${%s}", s.getName()));
                                    PsiDocumentManager.getInstance(project).commitDocument(document);
                                    AbstractAzureFacetNode.selectConnectedResource(connection, s.getId(), true);
                                })
                            ));
                        }
                    }).create();
            }
        }
    }
}
