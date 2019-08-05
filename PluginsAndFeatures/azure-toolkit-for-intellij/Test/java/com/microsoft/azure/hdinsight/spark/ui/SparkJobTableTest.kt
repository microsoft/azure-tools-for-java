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

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitResponse
import com.microsoft.azure.hdinsight.spark.common.SparkUITest
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobTableModel
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobTableModel.JobPage
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobTableViewport
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobViewer
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.UniqueColumnNameTableSchema
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.Operation
import org.junit.Ignore
import org.junit.Test
import rx.Observable
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.TimeUnit

class KillLivyJobAction : AzureAnAction(AllIcons.Actions.Cancel) {
    override fun onActionPerformed(anActionEvent: AnActionEvent, operation: Operation?): Boolean {
        System.out.println("Clicked ${anActionEvent.place} kill job button")
        return true
    }
}

class RestartLivyJobAction : AzureAnAction(AllIcons.Actions.Restart) {
    override fun onActionPerformed(anActionEvent: AnActionEvent, operation: Operation?): Boolean {
        System.out.println("Clicked ${anActionEvent.place} restart job button")
        return true
    }
}

const val killActionColName = "KillAction"
const val restartActionColName = "RestartAction"
const val idColName = "ID"
const val appIdColName = "AppID"
const val stateColName = "State"

class MockSparkLivyJobsTableSchema
    : UniqueColumnNameTableSchema(arrayOf(
        ActionColumnInfo(killActionColName),
        ActionColumnInfo(restartActionColName),
        PlainColumnInfo(idColName),
        PlainColumnInfo(appIdColName),
        PlainColumnInfo(stateColName))) {

    inner class MockSparkJobDescriptor(val jobStatus: SparkSubmitResponse) : RowDescriptor(
            killActionColName to KillLivyJobAction(),
            restartActionColName to RestartLivyJobAction(),
            idColName to jobStatus.id,
            appIdColName to jobStatus.appId,
            stateColName to jobStatus.state)

}

class MockSparkBatchJobViewerControl(private val view: MockSparkBatchJobViewer) : LivyBatchJobViewer.Control {
    override fun onNextPage(nextPageLink: String?): JobPage? {
        return getJobListPage(nextPageLink)
    }

    override fun onJobSelected(jobSelected: UniqueColumnNameTableSchema.RowDescriptor?) {
        val sparkJobDesc = (jobSelected as? MockSparkLivyJobsTableSchema.MockSparkJobDescriptor)?.let { arrayOf(it)}
            ?: emptyArray()

        Observable.from(sparkJobDesc)
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribe { view.getModel(LivyBatchJobViewer.Model::class.java).apply {
                    jobDetail = if (it.jobStatus.id == 1) {
                        // Unclosed JSON string
                        """{"message":"A broken response for ${it.jobStatus.appId}!","error no": 0, "id": ${it.jobStatus.id}"""

                    } else {
                        """{"message":"hello ${it.jobStatus.appId}!","error no": 0, "id": ${it.jobStatus.id}}"""
                    }

                    view.setData(this)
                }}
    }
}


class MockSparkBatchJobViewer : LivyBatchJobViewer() {
    override val jobViewerControl: Control by lazy { MockSparkBatchJobViewerControl(this@MockSparkBatchJobViewer) }
}

val jobView = MockSparkBatchJobViewer()
val tableSchema = MockSparkLivyJobsTableSchema()

fun parseJSON(json: String): SparkSubmitResponse {
    return ObjectMapper().readValue(json, SparkSubmitResponse::class.java)
}

fun getJobListPage(pageLink: String?): JobPage? {
    println("Get job list from $pageLink")

    return when (pageLink) {
        "http://page1" -> object : JobPage {
            override fun nextPageLink(): String? {
                return "http://page2"
            }

            override fun items(): List<UniqueColumnNameTableSchema.RowDescriptor>? {
                return listOf(
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 1,
                           "appId": "application-134124194-1",
                           "state": "running"
                        }""".trimIndent())),
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 2,
                           "appId": null,
                           "state": "dead"
                        }""".trimIndent())),
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 3,
                           "state": "success"
                        }""".trimIndent())),
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 4,
                           "appId": "application-134124194-4"
                        }""".trimIndent()))
                )
            }
        }
        "http://page2" -> object : JobPage {
            override fun nextPageLink(): String? {
                return "http://page3"
            }

            override fun items(): List<UniqueColumnNameTableSchema.RowDescriptor>? {
                return listOf(
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 5,
                           "appId": "application-134124194-5",
                           "state": "running"
                        }""".trimIndent())),
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 6,
                           "appId": null,
                           "state": "dead"
                        }""".trimIndent())),
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 7,
                           "state": "success"
                        }""".trimIndent())),
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 8,
                           "appId": "application-134124194-8"
                        }""".trimIndent()))
                )
            }
        }
        "http://page3" -> object : JobPage {
            override fun nextPageLink(): String? {
                return null
            }

            override fun items(): List<UniqueColumnNameTableSchema.RowDescriptor>? {
                return listOf(
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 9,
                           "appId": "application-134124194-9",
                           "state": "running"
                        }""".trimIndent())),
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 10,
                           "appId": null,
                           "state": "dead"
                        }""".trimIndent())),
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 11,
                           "state": "success"
                        }""".trimIndent())),
                        tableSchema.MockSparkJobDescriptor(parseJSON("""{
                           "id": 12,
                           "appId": "application-134124194-12"
                        }""".trimIndent()))
                )
            }
        }
        else -> null
    }
}
@Ignore
class SparkJobTableTest : SparkUITest() {

    @Test
    fun testLivyTable() {
        val model = LivyBatchJobViewer.Model(LivyBatchJobTableViewport.Model(
                    LivyBatchJobTableModel(tableSchema), getJobListPage("http://page1")))

        jobView.setData(model)

        dialog!!.apply {
            contentPane.add(jobView.component)
            pack()

            addWindowListener(object: WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    jobView.dispose()
                }
            })
            isVisible = true
        }
    }
}