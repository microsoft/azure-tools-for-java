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
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationUtil;
import com.intellij.execution.junit.JavaRunConfigurationProducerBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
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
import com.microsoft.azure.hdinsight.spark.run.action.SelectSparkApplicationTypeAction;
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType;
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition;
import scala.Option;
import scala.Tuple2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;
import java.util.Set;

public class SparkBatchJobLocalRunConfigurationProducer extends JavaRunConfigurationProducerBase<LivySparkBatchJobRunConfiguration> {
    private SparkApplicationType applicationType;


    public SparkBatchJobLocalRunConfigurationProducer(ConfigurationType configType, SparkApplicationType applicationType) {
        super(configType);
        this.applicationType = applicationType;
    }

    @Override
    public boolean setupConfigurationFromContext(LivySparkBatchJobRunConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement) {
        if (SelectSparkApplicationTypeAction.getSelectedSparkApplicationType() != this.applicationType) {
            return false;
        } else {
            return Optional.ofNullable(context.getModule())
                    .map(Module::getProject)
                    .flatMap(project -> getMainClassFromContext(context)
                            .filter(mcPair -> isSparkContext(project, mcPair.getKey().getContainingFile())))
                    .map(mcPair -> {
                        setupConfiguration(configuration, mcPair.getValue(), context);

                        return true;
                    })
                    .orElse(false);
        }
    }

    private boolean isSparkContext(Project project, PsiFile sourceFile) {
        // To determine if the current context has Spark Context dependence
        DependenciesBuilder db = new ForwardDependenciesBuilder(
                project, new AnalysisScope(sourceFile));

        db.analyze();

        return Optional.ofNullable(db.getDependencies().get(sourceFile))
                .map((Set<PsiFile> t) -> t.stream()
                        .map(PsiFile::getVirtualFile)
                        .map(VirtualFile::getNameWithoutExtension)
                        .anyMatch(className -> className.equals("SparkContext") ||
                                className.equals("JavaSparkContext") ||
                                className.equals("SparkConf") ||
                                className.equals("StreamingContext") ||
                                className.equals("SparkSession")))
                .orElse(false);
    }

    private void setupConfiguration(LivySparkBatchJobRunConfiguration configuration, final PsiClass clazz, final ConfigurationContext context) {
        SparkBatchJobConfigurableModel jobModel = configuration.getModel();

        getNormalizedClassName(clazz)
                .ifPresent(mainClass -> {
                    jobModel.getSubmitModel().getSubmissionParameter().setClassName(mainClass);
                    jobModel.getLocalRunConfigurableModel().setRunClass(mainClass);
                });

        configuration.setGeneratedName();
        configuration.setActionProperty(LivySparkBatchJobRunConfiguration.ACTION_TRIGGER_PROP, "Context");
        setupConfigurationModule(context, configuration);
    }

    private static Optional<String> getNormalizedClassName(@NotNull PsiClass clazz) {
        return Optional.ofNullable(JavaExecutionUtil.getRuntimeQualifiedName(clazz))
                       .map(mainClass -> mainClass.substring(
                               0,
                               Optional.of(mainClass.lastIndexOf('$'))
                                       .filter(o -> o >= 0)
                                       .orElse(mainClass.length())));
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

    private static Optional<SimpleImmutableEntry<PsiElement, PsiClass>> findJavaMainClass(PsiElement element) {
        return Optional.ofNullable(ApplicationConfigurationType.getMainClass(element))
                .map(clazz -> new SimpleImmutableEntry<PsiElement, PsiClass>(clazz, clazz));
    }

    private static Optional<SimpleImmutableEntry<PsiElement, PsiClass>> findScalaMainClass(PsiElement element) {
        // TODO: Replace with the following code after IDEA 2018.1
        // Option<Tuple2<PsiClass, PsiElement>> ceOption = ScalaMainMethodUtil.findMainClassAndSourceElem(element);
        try {
            // Added from IDEA 2017.2
            Method findMainClassAndSourceElemMethod = Class
                    .forName("org.jetbrains.plugins.scala.runner.ScalaMainMethodUtil")
                    .getDeclaredMethod("findMainClassAndSourceElem", PsiElement.class);

            Object option = findMainClassAndSourceElemMethod.invoke(null, element);
            if (option instanceof scala.None$ || !(option instanceof Option)) {
                return Optional.empty();
            }

            Option<Tuple2<PsiClass, PsiElement>> ceOption = (Option<Tuple2<PsiClass, PsiElement>>) option;

            return ceOption.isDefined() ?
                    Optional.of(new SimpleImmutableEntry<>(ceOption.get()._1(), ceOption.get()._1())) :
                    Optional.empty();
        } catch (NoSuchMethodException ignored) {
            // Use old one for IDEA 2017.1
            try {
                Method findContainingMainMethod = Class
                        .forName("org.jetbrains.plugins.scala.runner.ScalaMainMethodUtil")
                        .getDeclaredMethod("findContainingMainMethod", PsiElement.class);

                Object option = findContainingMainMethod.invoke(null, element);
                if (option instanceof scala.None$ || !(option instanceof Option)) {
                    return Optional.empty();
                }

                Option<ScFunctionDefinition> funDefOption = (Option<ScFunctionDefinition>) option;

                return funDefOption.isDefined() ?
                        Optional.of(new SimpleImmutableEntry<PsiElement, PsiClass>(
                                funDefOption.get().containingClass(),
                                funDefOption.get().containingClass())) :
                        Optional.empty();
            } catch (Exception ignore) {
                return Optional.empty();
            }
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<SimpleImmutableEntry<PsiElement, PsiClass>> getMainClassFromContext(ConfigurationContext context) {
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
                        Optional<SimpleImmutableEntry<PsiElement, PsiClass>> ccPair = findJavaMainClass(element);

                        return ccPair.isPresent() ? ccPair : findScalaMainClass(element);
                    }
                });
    }

    /**
     * The function to help reuse RunConfiguration
     * @param jobConfiguration Run Configuration to test
     * @param context current Context
     * @return true for reusable
     */
    @Override
    public boolean isConfigurationFromContext(LivySparkBatchJobRunConfiguration jobConfiguration, ConfigurationContext context) {
        return getMainClassFromContext(context)
                .filter(mcPair -> getNormalizedClassName(mcPair.getValue())
                            .map(name -> name.equals(jobConfiguration.getModel().getLocalRunConfigurableModel().getRunClass()))
                            .orElse(false))
                .filter(mcPair -> Optional.of(mcPair.getKey())
                            .filter(e -> e instanceof PsiMethod)
                            .map(PsiMethod.class::cast)
                            .map(method -> !TestFrameworks.getInstance().isTestMethod(method))
                            .orElse(true))
                .map(mcPair -> {
                    final Module configurationModule = jobConfiguration.getConfigurationModule().getModule();

                    if (!Comparing.equal(context.getModule(), configurationModule)) {

                        LivySparkBatchJobRunConfiguration template = (LivySparkBatchJobRunConfiguration)context
                                .getRunManager()
                                .getConfigurationTemplate(getConfigurationFactory())
                                .getConfiguration();
                        final Module predefinedModule = template.getConfigurationModule().getModule();

                        if (!Comparing.equal(predefinedModule, configurationModule)) {
                            return false;
                        }
                    }

                    jobConfiguration.setActionProperty(LivySparkBatchJobRunConfiguration.ACTION_TRIGGER_PROP, "ContextReuse");
                    return true;
                })
                .orElse(false);
    }

    @Override
    public boolean shouldReplace(@NotNull ConfigurationFromContext self, @NotNull ConfigurationFromContext anyOther) {
        return true;
    }
}
