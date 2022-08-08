/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.GradleProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.MavenProjectModule;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.gradle.util.GradleVersion;
import org.jetbrains.idea.maven.dom.MavenDomBundle;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.plugins.gradle.model.ExternalDependency;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.util.GradleUtil;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCommandArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DependencyUtils {

    public static void addOrUpdateMavenDependency(@Nonnull final MavenProjectModule module, @Nonnull final AzureSdkArtifactEntity entity, @Nonnull final String version) {
        final Project project = module.getProject();
        final MavenProject mavenProject = module.getMavenProject();
        final MavenDomProjectModel model = ReadAction.compute(() -> MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile()));
        if (model == null) {
            throw new AzureToolkitRuntimeException(String.format("Can not find build file for module %s", module.getName()));
        }
        WriteCommandAction.writeCommandAction(project, new PsiFile[]{DomUtil.getFile(model)}).withName(MavenDomBundle.message("maven.dom.quickfix.add.maven.dependency")).run(() -> {
            final MavenDomDependency existingDependency = model.getDependencies().getDependencies().stream()
                    .filter(dependency -> StringUtils.equalsIgnoreCase(dependency.getGroupId().getStringValue(), entity.getGroupId()) &&
                            StringUtils.equalsIgnoreCase(dependency.getArtifactId().getStringValue(), entity.getArtifactId()))
                    .findFirst().orElse(null);
            if (existingDependency == null) {
                final MavenId mavenId = new MavenId(entity.getGroupId(), entity.getArtifactId(), version);
                final MavenDomDependency dependency = MavenDomUtil.createDomDependency(model, null, mavenId);
            } else {
                existingDependency.getVersion().setStringValue(version);
            }
        });
        final AnAction action = ActionManager.getInstance().getAction("Maven.Reimport");
        final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
        AzureTaskManager.getInstance().runLater(() -> ActionUtil.invokeAction(action, context, "AzureSdkReferenceBook", null, null));
        AzureMessager.getMessager().info(AzureString.format("Library %s with version %s has been added to project %s", entity.getArtifactId(), version, module.getName()));
    }

    public static void addOrUpdateGradleDependency(@Nonnull final GradleProjectModule module, @Nonnull final AzureSdkArtifactEntity entity, @Nonnull final String version) {
        final Project project = module.getProject();
        final ExternalProject externalProject = module.getExternalProject();
        final File buildFile = externalProject.getBuildFile();
        if (buildFile == null) {
            throw new AzureToolkitRuntimeException(String.format("Can not find build file for module %s", module.getName()));
        }
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(buildFile.getAbsolutePath());
        final PsiFile file = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(Objects.requireNonNull(virtualFile)));
        WriteCommandAction.writeCommandAction(project, file).run(() -> {
            final GradleVersion gradleVersion = GradleUtil.getGradleVersion(project, file);
            final String scope = GradleUtil.isSupportedImplementationScope(gradleVersion) ? "implementation" : "compile";
            final GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);
            final List<GrMethodCall> closableBlocks = PsiTreeUtil.getChildrenOfTypeAsList(file, GrMethodCall.class);
            GrCall dependenciesBlock = ContainerUtil.find(closableBlocks, call -> {
                final GrExpression expression = call.getInvokedExpression();
                return "dependencies".equals(expression.getText());
            });
            final String dependencies = String.format("%s '%s'", scope, String.join(":", entity.getGroupId(), entity.getArtifactId(), version));
            if (dependenciesBlock == null) {
                dependenciesBlock = (GrCall) factory.createStatementFromText("dependencies{\n" + dependencies + "}");
                file.add(dependenciesBlock);
            } else {
                final ExternalDependency gradleDependency = module.getGradleDependency(entity.getGroupId(), entity.getArtifactId());
                final GrClosableBlock existingDependency = ArrayUtil.getFirstElement(dependenciesBlock.getClosureArguments());
                if (existingDependency != null) {
                    final GrApplicationStatement statement = getStatement(existingDependency, entity.getGroupId(), entity.getArtifactId());
                    if (statement != null) {
                        statement.removeStatement();
                    }
                    existingDependency.addStatementBefore(factory.createStatementFromText(dependencies), null);
                }
            }
        });
        final AnAction action = ActionManager.getInstance().getAction("ExternalSystem.RefreshAllProjects");
        final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
        AzureTaskManager.getInstance().runLater(() -> ActionUtil.invokeAction(action, context, "AzureSdkReferenceBook", null, null));
        AzureMessager.getMessager().info(AzureString.format("Library %s with version %s has been added to project %s", entity.getArtifactId(), version, module.getName()));
    }

    @Nullable
    private static GrApplicationStatement getStatement(final GrClosableBlock dependencyBlock, final String groupId, final String artifactId) {
        return Arrays.stream(dependencyBlock.getChildren())
                .filter(psiElement -> psiElement instanceof GrApplicationStatement)
                .map(object -> (GrApplicationStatement) object)
                .filter(statement -> getArgumentValues(statement).containsAll(Arrays.asList(groupId, artifactId)))
                .findFirst().orElse(null);
    }

    private static List<String> getArgumentValues(final GrApplicationStatement statement) {
        final GrCommandArgumentList argumentList = statement.getArgumentList();
        final GroovyPsiElement[] allArguments = argumentList.getAllArguments();
        return Arrays.stream(allArguments).map(argument -> argument instanceof GrNamedArgument ?
                        Optional.ofNullable(((GrNamedArgument) argument).getExpression()).map(GrExpression::getText).orElse(null) : argument.getText())
                .filter(Objects::nonNull)
                .map(value -> value.replaceAll("[\"']", ""))
                .flatMap(value -> Arrays.stream(value.split(":")))
                .collect(Collectors.toList());
    }
}
