/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

package com.microsoft.intellij.helpers.base

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.jetbrains.rd.platform.util.application
import com.jetbrains.rider.util.idea.getLogger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reactive.Signal
import com.jetbrains.rd.util.reactive.adviseOnce
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter
import com.microsoft.azuretools.core.mvp.ui.base.MvpView
import com.microsoft.tooling.msservices.components.DefaultLoader

open class AzureMvpPresenter<V : MvpView> : MvpPresenter<V>() {

    companion object {
        private val LOG = getLogger(this)
    }

    fun <T>subscribe(lifetime: Lifetime,
                     signal: Signal<T>,
                     taskName: String,
                     errorMessage: String,
                     callableFunc: () -> T,
                     invokeLaterCallback: (T) -> Unit) {

        signal.adviseOnce(lifetime) {
            application.invokeLater {
                if (!lifetime.isAlive) return@invokeLater
                invokeLaterCallback(it)
            }
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, taskName, false) {
            override fun run(indicator: ProgressIndicator) {
                signal.fire(callableFunc())
            }

            override fun onThrowable(e: Throwable) {
                LOG.error(e)
                errorHandler(errorMessage, e as Exception)
                super.onThrowable(e)
            }
        })
    }

    private fun errorHandler(message: String, e: Exception) {
        DefaultLoader.getIdeHelper().invokeLater {
            if (isViewDetached) return@invokeLater
            mvpView.onErrorWithException(message, e)
        }
    }
}