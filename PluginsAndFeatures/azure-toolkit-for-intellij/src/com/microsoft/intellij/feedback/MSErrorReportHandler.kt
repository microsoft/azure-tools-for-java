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
                                event.toString())
                        .with("Additional Info", additionalInfo ?: "None")
                        .with("Parent component", parentComponent.toString()))
                .withLabel("bug")

        githubIssue.report()

        // TODO: Check if there is duplicated issue

        val reportInfo = SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE)
        callback.consume(reportInfo)

        return true
    }
}