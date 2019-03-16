/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.functions

import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.FunctionAppNode

class FunctionNode(parentNode: FunctionAppNode,
                   override val subscriptionId: String,
                   override var isEnabled: Boolean,
                   override val functionId: String,
                   override val functionName: String) :
        Node(functionId, functionName, parentNode, getFunctionIcon(isEnabled), true),
        FunctionVirtualInterface,
        FunctionNodeView {

    companion object {
        private const val ICON_FUNCTION = "Function.svg"
        private const val ICON_FUNCTION_DISABLED = "FunctionDisabled.svg"

        private fun getFunctionIcon(isEnabled: Boolean) =
                if (isEnabled) ICON_FUNCTION
                else ICON_FUNCTION_DISABLED
    }

    private val presenter = FunctionNodePresenter<FunctionNode>()

    init {
        presenter.onAttachView(this@FunctionNode)
    }

    override fun renderNode(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        setIconPath(getFunctionIcon(isEnabled))
    }
}
