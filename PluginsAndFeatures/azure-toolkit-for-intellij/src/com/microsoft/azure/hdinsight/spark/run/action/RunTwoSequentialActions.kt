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

package com.microsoft.azure.hdinsight.spark.run.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azuretools.ijidea.utility.AzureAnAction

open class RunTwoSequentialActions(private val firstActionId: String, private val secondActionId: String): AzureAnAction(), ILogger {
    override fun onActionPerformed(anActionEvent: AnActionEvent?) {
        if(anActionEvent != null) {
            val firstAction = ActionManagerEx.getInstance().getAction(firstActionId)
            firstAction?.actionPerformed(anActionEvent)
                    ?: log().error("Can't perform the first action with id $firstActionId")
            val secondAction = ActionManagerEx.getInstance().getAction(secondActionId)
            secondAction?.actionPerformed(anActionEvent)
                    ?: log().error("Can't perform the second action with id $secondActionId")
        }
    }
}