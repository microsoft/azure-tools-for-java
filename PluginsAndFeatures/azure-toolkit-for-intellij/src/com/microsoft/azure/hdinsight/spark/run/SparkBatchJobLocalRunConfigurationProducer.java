/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.TestFrameworks;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationUtil;
import com.intellij.execution.junit.JavaRunConfigurationProducerBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.ModuleImpl;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DependenciesBuilder;
import com.intellij.packageDependencies.ForwardDependenciesBuilder;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiMethodUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJobConfigurableModel;
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfiguration;
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.runner.ScalaMainMethodUtil;
import scala.Option;
import scala.Tuple2;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;
import java.util.Set;

public class SparkBatchJobLocalRunConfigurationProducer extends JavaRunConfigurationProducerBase<RemoteDebugRunConfiguration> {
    public SparkBatchJobLocalRunConfigurationProducer() {
        super(RemoteDebugRunConfigurationType.getInstance());
    }

    @Override
    protected boolean setupConfigurationFromContext(RemoteDebugRunConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement) {
        final Optional<Location> location = Optional.ofNullable(context.getLocation());

        return location
                .map(JavaExecutionUtil::stepIntoSingleClass)
                .map(Location::getPsiElement)
                .filter(PsiElement::isPhysical)
                .flatMap(element -> {
                    Optional<SimpleImmutableEntry<PsiElement, PsiClass>> mcPair = findMainMethod(element);

                    if (mcPair.isPresent()) {
                        return mcPair;
                    } else {
                        Optional<SimpleImmutableEntry<PsiElement, PsiClass>> ccPair = findMainClass(element);

                        return ccPair.isPresent() ? ccPair : findScalaMainClass(element);
                    }
                })
                .filter(mcPair -> {
                    // To determine if the current context has Spark Context dependence
                    DependenciesBuilder db = new ForwardDependenciesBuilder(
                            context.getModule().getProject(), new AnalysisScope(mcPair.getKey().getContainingFile()));

                    db.analyze();

                    return Optional.ofNullable(db.getDependencies().get(mcPair.getKey().getContainingFile()))
                            .map((Set<PsiFile> t) -> t.stream()
                                    .map(PsiFile::getVirtualFile)
                                    .map(VirtualFile::getName)
                                    .anyMatch(className -> className.equals("SparkContext.class") || className.equals("JavaSparkContext.class")))
                            .orElse(false);
                })
                .map(mcPair -> {
                    sourceElement.set(mcPair.getKey());
                    setupConfiguration(configuration, mcPair.getValue(), context);

                    return true;
                })
                .orElse(false);
    }

    private void setupConfiguration(RemoteDebugRunConfiguration configuration, final PsiClass clazz, final ConfigurationContext context) {
        SparkBatchJobConfigurableModel jobModel = configuration.getModel();
        String mainClass = JavaExecutionUtil.getRuntimeQualifiedName(clazz);

        mainClass = mainClass.substring(0, Optional.of(mainClass.lastIndexOf('$')).filter(o -> o >= 0).orElse(mainClass.length()));

        jobModel.getSubmitModel().getSubmissionParameter().setClassName(mainClass);
        jobModel.getLocalRunConfigurableModel().setRunClass(mainClass);
        configuration.setGeneratedName();
        setupConfigurationModule(context, configuration);
    }

    private static Optional<SimpleImmutableEntry<PsiElement, PsiClass>> findMainMethod(PsiElement element) {
        PsiMethod method;

        while ((method = PsiTreeUtil.getParentOfType(element, PsiMethod.class)) != null) {
            if (PsiMethodUtil.isMainMethod(method)) {
                return Optional.of(new SimpleImmutableEntry<PsiElement, PsiClass>(method, method.getContainingClass()))
                        .filter(pair -> ConfigurationUtil.MAIN_CLASS.value(pair.getValue()));
            }

            element = method.getParent();
        }

        return Optional.empty();
    }

    private static Optional<SimpleImmutableEntry<PsiElement, PsiClass>> findMainClass(PsiElement element) {
        return Optional.ofNullable(ApplicationConfigurationType.getMainClass(element))
                .map(clazz -> new SimpleImmutableEntry<PsiElement, PsiClass>(clazz, clazz));
    }

    private static Optional<SimpleImmutableEntry<PsiElement, PsiClass>> findScalaMainClass(PsiElement element) {
        Option<Tuple2<PsiClass, PsiElement>> ceOption = ScalaMainMethodUtil.findMainClassAndSourceElem(element);

        return ceOption.isDefined() ?
                Optional.of(new SimpleImmutableEntry<>(ceOption.get()._1(), ceOption.get()._1())) :
                Optional.empty();
    }

    @Override
    public boolean isConfigurationFromContext(RemoteDebugRunConfiguration jobConfiguration, ConfigurationContext context) {
        SparkBatchJobConfigurableModel jobModel = jobConfiguration.getModel();

        final PsiElement location = context.getPsiLocation();
        final PsiClass clazz = ApplicationConfigurationType.getMainClass(location);
        if (clazz != null &&
                Comparing.equal(JavaExecutionUtil.getRuntimeQualifiedName(clazz),
                                jobModel.getLocalRunConfigurableModel().getRunClass())) {
            final PsiMethod method = PsiTreeUtil.getParentOfType(location, PsiMethod.class, false);
            if (method != null && TestFrameworks.getInstance().isTestMethod(method)) {
                return false;
            }

            final Module configurationModule = jobConfiguration.getConfigurationModule().getModule();
            if (Comparing.equal(context.getModule(), configurationModule)) {
                return true;
            }

            ApplicationConfiguration template = (ApplicationConfiguration)context
                    .getRunManager()
                    .getConfigurationTemplate(getConfigurationFactory())
                    .getConfiguration();
            final Module predefinedModule = template.getConfigurationModule().getModule();
            if (Comparing.equal(predefinedModule, configurationModule)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldReplace(@NotNull ConfigurationFromContext self, @NotNull ConfigurationFromContext other) {
        return true;
    }
}
