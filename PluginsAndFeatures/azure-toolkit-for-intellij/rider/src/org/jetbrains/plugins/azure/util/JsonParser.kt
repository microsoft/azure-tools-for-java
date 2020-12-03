/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.util

import com.intellij.json.psi.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.containers.ContainerUtil

object JsonParser {

    fun jsonFileFromVirtualFile(virtualFile: VirtualFile, project: Project): JsonFile? {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return null
        val file = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null
        return file as? JsonFile
    }

    fun writeProperty(project: Project,
                      jsonObject: JsonObject,
                      propertyName: String,
                      propertyValue: String,
                      escapeValue: Boolean = true): JsonProperty? {
        val generator = JsonElementGenerator(project)
        val property =
            if (escapeValue) {
                generator.createProperty(propertyName, "\"" + StringUtil.escapeStringCharacters(propertyValue) + "\"")
            } else {
                generator.createProperty(propertyName, propertyValue)
            }

        val existingProperty = jsonObject.findProperty(propertyName)
        if (existingProperty != null) {
            existingProperty.replace(property)
            return property
        }

        val list = jsonObject.propertyList
        return if (list.isEmpty()) {
            jsonObject.addAfter(property, jsonObject.firstChild)
            property
        } else {
            val comma = jsonObject.addAfter(generator.createComma(), ContainerUtil.getLastItem(list))
            jsonObject.addAfter(property, comma) as JsonProperty
        }
    }

    fun findStringProperty(jsonObject: JsonObject?, propertyName: String): String? =
        findProperty<JsonStringLiteral>(jsonObject, propertyName)?.value

    fun findBooleanProperty(jsonObject: JsonObject?, propertyName: String): Boolean? =
        findProperty<JsonBooleanLiteral>(jsonObject, propertyName)?.value

    fun findNumberProperty(jsonObject: JsonObject?, propertyName: String): Number? =
        findProperty<JsonNumberLiteral>(jsonObject, propertyName)?.value

    private inline fun <reified T> findProperty(jsonObject: JsonObject?, propertyName: String): T? {
        val property = jsonObject?.findProperty(propertyName) ?: return null
        return property.value as? T
    }
}
