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

package com.microsoft.intellij.helpers.functionapp

import com.intellij.openapi.project.Project
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.FunctionAppPropertyMvpView
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.FunctionAppPropertyViewPresenter
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.base.FunctionAppBasePropertyViewPresenter

class FunctionAppPropertyView(project: Project, subscriptionId: String, appId: String) :
        FunctionAppBasePropertyView(project, subscriptionId, appId, "") {

    companion object {

        private const val PROPERTY_ID = "com.microsoft.intellij.helpers.functionapp.FunctionAppPropertyView"

        /**
         * Initialize the Web App Property View and return it.
         */
        fun create(project: Project, subscriptionId: String, appId: String): FunctionAppBasePropertyView {
            val view = FunctionAppPropertyView(project, subscriptionId, appId)
            view.onLoadFunctionAppProperty(subscriptionId, appId, "")
            return view
        }
    }

    override fun getId() = PROPERTY_ID

    override fun createPresenter(): FunctionAppBasePropertyViewPresenter<FunctionAppPropertyMvpView> =
            FunctionAppPropertyViewPresenter()
}