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

package com.microsoft.intellij.feedback

import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import java.awt.Component

class MSErrorReportHandler : ErrorReportSubmitter() {
    override fun getReportActionText(): String {
        return "Report to Microsoft"
    }

    override fun submit(events: Array<out IdeaLoggingEvent>,
                        additionalInfo: String?,
                        parentComponent: Component,
                        callback: Consumer<SubmittedReportInfo>): Boolean {
        val event = events[0]

        val githubIssue = GithubIssue(
                ReportableError("Uncaught Exception ${event.message ?: ""} ${event.throwableText.split("\n").first()}",
                                filterMSCallStacks(event.toString()))
                        .with("Additional Info", additionalInfo ?: "None")
                        .with("Parent component", GithubMarkdownFormat.toCode(parentComponent.toString())))
                .withLabel("bug")

        githubIssue.report()

        // TODO: Check if there is duplicated issue

        val reportInfo = SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE)
        callback.consume(reportInfo)

        return true
    }
}

fun filterMSCallStacks(callStacks: String): String {
    // The call stack looks like:
    // 	java.lang.IllegalStateException: Exception thrown on Scheduler.Worker thread. Add `onError` handling.
    //		at rx.internal.schedulers.ScheduledAction.run(ScheduledAction.java:57)
    //		at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
    //		at java.util.concurrent.FutureTask.run(FutureTask.java:266)
    //		at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$201(ScheduledThreadPoolExecutor.java:180)
    //		at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:293)
    //		at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
    //		at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
    //		at java.lang.Thread.run(Thread.java:748)
    //	Caused by: rx.exceptions.OnErrorNotImplementedException: Coordinate out of bounds!
    //		at rx.internal.util.InternalObservableUtils$ErrorNotImplementedAction.call(InternalObservableUtils.java:386)
    //		at rx.internal.util.InternalObservableUtils$ErrorNotImplementedAction.call(InternalObservableUtils.java:383)
    //		at rx.internal.util.ActionSubscriber.onError(ActionSubscriber.java:44)
    //		at rx.observers.SafeSubscriber._onError(SafeSubscriber.java:153)
    //		at rx.observers.SafeSubscriber.onError(SafeSubscriber.java:115)
    //		at rx.exceptions.Exceptions.throwOrReport(Exceptions.java:212)
    //		at rx.observers.SafeSubscriber.onNext(SafeSubscriber.java:139)
    //		at rx.internal.operators.OperatorTake$1.onNext(OperatorTake.java:79)
    //		at rx.internal.operators.OperatorSubscribeOn$SubscribeOnSubscriber.onNext(OperatorSubscribeOn.java:74)
    //		at rx.internal.operators.OnSubscribeTimerOnce$1.call(OnSubscribeTimerOnce.java:49)
    //		at rx.internal.schedulers.EventLoopsScheduler$EventLoopWorker$2.call(EventLoopsScheduler.java:189)
    //		at rx.internal.schedulers.ScheduledAction.run(ScheduledAction.java:55)
    //		... 7 more
    //	Caused by: java.lang.ArrayIndexOutOfBoundsException: Coordinate out of bounds!
    //		at sun.awt.image.IntegerInterleavedRaster.getDataElements(IntegerInterleavedRaster.java:264)
    //		at sun.awt.image.OffScreenImageSource.sendPixels(OffScreenImageSource.java:136)
    //		at sun.awt.image.OffScreenImageSource.produce(OffScreenImageSource.java:187)
    //		at sun.awt.image.OffScreenImageSource.addConsumer(OffScreenImageSource.java:66)
    //		at sun.awt.image.OffScreenImageSource.startProduction(OffScreenImageSource.java:80)
    //		at java.awt.image.FilteredImageSource.startProduction(FilteredImageSource.java:183)
    //		at sun.awt.image.ImageRepresentation.startProduction(ImageRepresentation.java:732)
    //		at sun.awt.image.ToolkitImage.addWatcher(ToolkitImage.java:221)
    //		at sun.awt.image.ToolkitImage.getProperty(ToolkitImage.java:169)
    //		at javax.swing.ImageIcon.<init>(ImageIcon.java:240)
    //		at com.microsoft.intellij.util.PluginUtil.getIcon(PluginUtil.java:183)
    //		at com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog.createUIComponents(SurveyPopUpDialog.java:223)
    //		at com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog.$$$setupUI$$$(SurveyPopUpDialog.java)
    //		at com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog.<init>(SurveyPopUpDialog.java:55)
    //		at com.microsoft.intellij.helpers.CustomerSurveyHelper.lambda$showFeedbackNotification$0(CustomerSurveyHelper.java:68)
    //		at rx.internal.util.ActionSubscriber.onNext(ActionSubscriber.java:39)
    //		at rx.observers.SafeSubscriber.onNext(SafeSubscriber.java:134)
    //		... 12 more
    //
    // Ignore the beginning 2 lines and `Caused by` line, only filter lines starting with `at`.

    return callStacks.splitToSequence("\n", "\r")
            .filter { it.isNotBlank() }
            .filterIndexed { index, line ->
                // The First and second non-blank lines
                if (index <= 1) {
                    return@filterIndexed true
                }

                // `Caused by` line
                if (line.matches("""^\s*Caused by:.*""".toRegex())) {
                    return@filterIndexed true
                }

                // `at` line
                if (line.matches("""^\s*at .*microsoft.*""".toRegex(RegexOption.IGNORE_CASE))) {
                    return@filterIndexed true
                }

                false
            }
            .joinToString("\n")
}
