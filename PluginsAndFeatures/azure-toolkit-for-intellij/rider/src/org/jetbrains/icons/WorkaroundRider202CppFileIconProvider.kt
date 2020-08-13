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

package org.jetbrains.icons

import com.intellij.ide.IconProvider
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.PathUtilRt.getFileExtension
import com.microsoft.intellij.helpers.UIHelperImpl
import javax.swing.Icon

// NOTE: This class should be removed after CppFileIconProvider is updated to not override icons
// for our editors (e.g. blob container browser)
@Deprecated(
        message = "This should be removed. See https://github.com/JetBrains/azure-tools-for-intellij/pull/352",
        level = DeprecationLevel.WARNING)
class WorkaroundRider202CppFileIconProvider : IconProvider() {

    override fun getIcon(element: PsiElement, @Iconable.IconFlags flags: Int): Icon? {
        val fileElement = element as? PsiFile ?: return null
        val ext = getFileExtension(fileElement.name)

        // Emulate CppFileIconProvider case for null / ""
        if (ext == null || ext == "") {
            // Check if it is a LightVirtualFile created by UIHelperImpl
            // (based on various keys)
            if (fileElement.virtualFile is LightVirtualFile) {
                if (fileElement.virtualFile.getUserData(UIHelperImpl.STORAGE_KEY) != null
                        || fileElement.virtualFile.getUserData(UIHelperImpl.CLIENT_STORAGE_KEY) != null
                        || fileElement.virtualFile.getUserData(UIHelperImpl.SUBSCRIPTION_ID) != null
                        || fileElement.virtualFile.getUserData(UIHelperImpl.RESOURCE_ID) != null
                        || fileElement.virtualFile.getUserData(UIHelperImpl.WEBAPP_ID) != null
                        || fileElement.virtualFile.getUserData(UIHelperImpl.APP_ID) != null
                        || fileElement.virtualFile.getUserData(UIHelperImpl.SLOT_NAME) != null) {

                    // Return the icon we expect
                    return fileElement.virtualFile.fileType.icon
                }
            }
        }
        return null
    }
}