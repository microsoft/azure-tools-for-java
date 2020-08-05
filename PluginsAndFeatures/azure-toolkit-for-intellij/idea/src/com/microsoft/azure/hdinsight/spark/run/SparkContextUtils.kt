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

package com.microsoft.azure.hdinsight.spark.run

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.TestFrameworks
import com.intellij.execution.JavaExecutionUtil
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationContext.SHARED_CONTEXT
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.packageDependencies.ForwardDependenciesBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiMethodUtil
import com.microsoft.azure.hdinsight.spark.console.SparkScalaPluginDelegate
import scala.Option
import scala.Tuple2
import java.lang.reflect.InvocationTargetException

val sparkContextKeywords = arrayOf(
        "SparkContext",
        "JavaSparkContext",
        "SparkConf",
        "StreamingContext",
        "SparkSession"
)

fun PsiFile.isSparkContext(): Boolean {
    // To determine if the current context has Spark Context dependence
    val db = ForwardDependenciesBuilder(project, AnalysisScope(this))

    db.analyze()

    return db.dependencies[this]
            ?.map { psFile -> psFile.virtualFile?.nameWithoutExtension }
            ?.any { className -> className in sparkContextKeywords }
            ?: false
}

fun PsiClass.getNormalizedClassNameForSpark(): String? {
    return JavaExecutionUtil.getRuntimeQualifiedName(this)?.substringBeforeLast('$')
}

fun DataContext.getSparkConfigurationContext(): ConfigurationContext? {
    val dataManager = DataManager.getInstance()
    return dataManager.loadFromDataContext(this, SHARED_CONTEXT)?.takeIf {
        it.getSparkMainClassWithElement()?.containingFile?.isSparkContext() == true
    }
}

fun DataContext.isSparkContext(): Boolean {
    return getSparkConfigurationContext() != null
}

fun ConfigurationContext.getSparkMainClassWithElement(): PsiClass? {
    return location?.let {
        val elem = JavaExecutionUtil.stepIntoSingleClass(it).psiElement.takeIf { psiElem -> psiElem.isPhysical }

        (elem as? PsiClass)?.findMainMethod()
                ?: elem?.findJavaMainClass()
                ?: elem?.findScalaObjectMainClass()
    }
}

fun PsiClass.findMainMethod(): PsiClass? {
    return PsiMethodUtil.findMainMethod(this)?.takeUnless { TestFrameworks.getInstance().isTestMethod(it) }
            ?.containingClass
}

fun PsiElement.findJavaMainClass(): PsiClass? {
    return ApplicationConfigurationType.getMainClass(this)?.takeUnless { TestFrameworks.getInstance().isTestClass(it) }
}

fun PsiElement.findScalaObjectMainClass(): PsiClass? {
    val scalaMainMethodUtilDelegate = SparkScalaPluginDelegate("org.jetbrains.plugins.scala.util.ScalaMainMethodUtil")

    if (!scalaMainMethodUtilDelegate.isEnabled) {
        return null
    }

    val findMainClassAndSourceElemMethod = scalaMainMethodUtilDelegate
            .getMethod("findMainClassAndSourceElem", PsiElement::class.java) ?: return null

    try {
        val option = findMainClassAndSourceElemMethod.invoke(null, this)
        if (option is scala.`None$` || option !is Option<*>) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        return ((option as Option<Tuple2<PsiClass, PsiElement>>).takeIf { it.isDefined })?.get()?._1()
                ?.takeUnless { TestFrameworks.getInstance().isTestClass(it) }
    } catch (ex: Exception) {
        when (ex) {
            is IllegalAccessException, is InvocationTargetException -> return null
            else -> throw ex
        }
    }
}

