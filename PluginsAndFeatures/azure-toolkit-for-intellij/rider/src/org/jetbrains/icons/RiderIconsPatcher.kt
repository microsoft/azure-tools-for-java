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

package org.jetbrains.icons

import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.IconPathPatcher
import com.intellij.ui.ScalableIconWrapperWithToolTip
import com.microsoft.icons.CommonIcons
import icons.RestClientIcons
import javax.swing.Icon

/**
 * Icons Patcher for icons set from Rider backend (R#).
 * Rider backend do not have access to fronted icons (e.g. FunctionApp.svg, new.svg, etc.). To share an existing frontend icons
 * and reuse them instead of creating a duplicate set for a backend, we can replace a fake backend icon with any frontend icon by path.
 */
internal class RiderIconsPatcher : IconPathPatcher() {

    companion object {
        fun install() = myInstallPatcher

        private val myInstallPatcher: Unit by lazy {
            IconLoader.installPathPatcher(RiderIconsPatcher())
        }

        private fun path(icon: Icon): String {
            val iconToProcess =
                    if (icon is ScalableIconWrapperWithToolTip) icon.retrieveIcon()
                    else icon

            val cachedIcon = iconToProcess as? IconLoader.CachedImageIcon
                    ?: throw RuntimeException("${icon.javaClass.simpleName} should be CachedImageIcon")

            return cachedIcon.originalPath
                    ?: throw RuntimeException("Unable to get original path for icon: ${cachedIcon.javaClass.simpleName}")
        }
    }

    override fun patchPath(path: String, classLoader: ClassLoader?): String? = myIconsOverrideMap[path]

    override fun getContextClassLoader(path: String, originalClassLoader: ClassLoader?): ClassLoader? =
        if (myIconsOverrideMap.containsKey(path)) javaClass.classLoader
        else originalClassLoader

    private val myIconsOverrideMap = mapOf(
        "/resharper/FunctionAppRunMarkers/RunFunctionApp.svg" to path(CommonIcons.AzureFunctions.FunctionAppRunConfiguration),
        "/resharper/FunctionAppRunMarkers/Trigger.svg" to path(RestClientIcons.Http_requests_filetype)
    )
}
