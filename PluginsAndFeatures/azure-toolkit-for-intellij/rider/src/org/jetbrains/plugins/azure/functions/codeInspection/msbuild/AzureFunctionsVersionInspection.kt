/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.codeInspection.msbuild

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementFactory
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.xml.XmlChildRole
import com.intellij.psi.xml.XmlElementType
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.XmlUtil

class AzureFunctionsVersionInspection : XmlSuppressableInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : XmlElementVisitor() {
            override fun visitXmlTag(tag: XmlTag?) {
                if (tag == null) return

                val tagName = tag.name.toLowerCase()
                if (tagName == "azurefunctionsversion") {

                    val child = XmlChildRole.START_TAG_END_FINDER.findChild(tag.node)
                            ?: XmlChildRole.EMPTY_TAG_END_FINDER.findChild(tag.node)
                    if (child != null) {
                        val node = child.treeNext
                        if (node == null || node.elementType !== XmlElementType.XML_TEXT) {
                            holder.registerProblem(tag,
                                    "Azure Functions version not specified",
                                    ProblemHighlightType.WARNING,

                                    // https://docs.microsoft.com/en-us/azure/azure-functions/functions-versions
                                    SetVersionQuickFix("v2"),
                                    SetVersionQuickFix("v3"))
                        }
                    }
                }
            }
        }
    }

    private class SetVersionQuickFix(val version: String) : LocalQuickFix {
        override fun getFamilyName(): String {
            return "Set Azure Functions version '$version'"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val tag = descriptor.psiElement as XmlTag

            XmlUtil.expandTag(tag)

            val newTag = XmlElementFactory.getInstance(tag.project)
                    .createTagFromText("<${tag.name}>$version</${tag.name}>")

            val node = tag.node as? CompositeElement ?: return

            node.replaceAllChildrenToChildrenOf(newTag.node)
        }
    }
}
