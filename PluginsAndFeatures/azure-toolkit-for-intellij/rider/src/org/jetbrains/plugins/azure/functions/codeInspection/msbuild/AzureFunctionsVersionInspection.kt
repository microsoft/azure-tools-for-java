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