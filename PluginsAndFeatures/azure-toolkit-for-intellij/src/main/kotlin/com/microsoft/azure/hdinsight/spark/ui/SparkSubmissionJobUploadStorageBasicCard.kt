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

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.mvc.IdeaSettableControlWithRwLock
import com.microsoft.azure.hdinsight.common.mvvm.Mvvm
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStorageBasicCard.Model
import com.microsoft.intellij.rxjava.DisposableObservers
import com.microsoft.intellij.rxjava.IdeaSchedulers
import com.microsoft.intellij.ui.util.UIUtils.assertInPooledThread
import org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage
import org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseStackTrace
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.properties.Delegates

abstract class SparkSubmissionJobUploadStorageBasicCard(val title: String)
    : Mvvm, IdeaSettableControlWithRwLock<Model>, Disposable, ILogger {
    companion object {
        private const val NOT_READY_PATH_PREFIX     = "<"
        private const val NOT_READY_PATH_POSTFIX    = ">"

        val INVALID_UPLOAD_PATH: String     = buildNotReadyPath("Invalid upload path")
        val IN_PROGRESS_CHECKING: String    = buildNotReadyPath("In progress of checking upload paths")

        fun buildNotReadyPath(message: String): String = "$NOT_READY_PATH_PREFIX $message $NOT_READY_PATH_POSTFIX"

        fun isNotReadyPath(message: String?): Boolean = message?.startsWith(NOT_READY_PATH_PREFIX) == true
    }

    interface Model: Mvvm.Model {
        var errorMsg: String?
    }

    override val model: Model
        get() = SparkSubmitJobUploadStorageModel().apply { getData(this) }

    //storage check event for storageCheckSubject in panel
    abstract class StorageCheckEvent(val message: String) {
        class SelectedClusterEvent(val cluster: IClusterDetail?, preCluster: IClusterDetail?)
            : StorageCheckEvent("Selected cluster ${cluster?.name} instead of previous ${preCluster?.title}")

        class SignInOutEvent(isSignIn: Boolean)
            : StorageCheckEvent("After user clicked sign ${if (isSignIn) "in" else "out"}")

        class PathInputFocusLostEvent(rootPathType: SparkSubmitStorageType)
            : StorageCheckEvent("$rootPathType root path focus lost")

        class InputFocusLostEvent(component: JComponent)
            : StorageCheckEvent("$component focus is lost")

        class InputChangedEvent(component: JComponent)
            : StorageCheckEvent("$component value is changed")

        class SelectedStorageTypeEvent(storageType: SparkSubmitStorageType)
            : StorageCheckEvent("Selected storage type: $storageType")
    }

    open inner class ViewModel: Mvvm.ViewModel, DisposableObservers() {
        val storageCheckSubject: PublishSubject<StorageCheckEvent> =
                disposableSubjectOf { PublishSubject.create() }

        open var cluster: IClusterDetail? by Delegates.observable(null as IClusterDetail?) { _, oldValue, newValue ->
            assertInPooledThread()

            storageCheckSubject.onNext(StorageCheckEvent.SelectedClusterEvent(newValue, oldValue))
            log().info("set cluster from ${oldValue?.title} to ${newValue?.title}")
        }

        val validatedStorageUploadUri: BehaviorSubject<String> = disposableSubjectOf {
            BehaviorSubject.create(INVALID_UPLOAD_PATH)
        }

        // validate config with returning uploading path
        @Throws(RuntimeConfigurationException::class)
        open fun getValidatedStorageUploadPath(config: Model): String = INVALID_UPLOAD_PATH
        private val ideaSchedulers = IdeaSchedulers()
        var errorMessage: String? = null

        init {
            storageCheckSubject
                    .doOnNext {
                        // Set error message to prevent user from applying the changes
                        // when validation is not completed
                        errorMessage = "validating storage info is not completed"
                        validatedStorageUploadUri.onNext(IN_PROGRESS_CHECKING)
                    }
                    .groupBy { checkEvent -> checkEvent::class.java.typeName}
                    .subscribe(
                            { groupedOb -> groupedOb
                                    .throttleWithTimeout(200, TimeUnit.MILLISECONDS)
                                    .doOnNext {
                                        log().info("Receive checking message ${it.message}")

                                        when (it) {
                                            is StorageCheckEvent.SelectedStorageTypeEvent -> onSelected()
                                            else -> {}
                                        }
                                    }
                                    .observeOn(ideaSchedulers.dispatchUIThread())
                                    .map { model }
                                    .observeOn(ideaSchedulers.dispatchPooledThread())
                                    .subscribe { model ->
                                        try {
                                            val uploadUri = getValidatedStorageUploadPath(model)

                                            log().info("Artifact uploading URI parsed from storage configuration: $uploadUri")
                                            validatedStorageUploadUri.onNext(uploadUri)
                                        } catch (err: Exception) {
                                            log().info("Checked result with error: ${err.message}")
                                            errorMessage = err.message ?: "unknown"
                                            validatedStorageUploadUri.onNext(buildNotReadyPath(err.message ?: "unknown"))
                                        }
                                    }
                            },
                            { err -> log().warn(getRootCauseMessage(err), getRootCauseStackTrace(err)) })
        }

        protected open fun onSelected() {
        }
    }

    private var myViewModel: ViewModel? = null

    // IntelliJ classloader can't handle well Kotlin's inner class lazy type, have to implement it manually
    final override val viewModel: ViewModel
        get() = myViewModel ?: synchronized(this) {
            myViewModel ?: createViewModel().apply {
                Disposer.register(this@SparkSubmissionJobUploadStorageBasicCard, this@apply)
                myViewModel = this
            }}

    protected abstract fun createViewModel(): ViewModel
    override val view: JComponent = JPanel()

    final override fun setDataInDispatch(from: Model) = super.setDataInDispatch(from)
    final override fun getData(to: Model) = super.getData(to)
    final override fun setData(from: Model) = super.setData(from)

    override fun readWithLock(to: Model) { }
    override fun writeWithLock(from: Model) { }

    override fun dispose() { }
}