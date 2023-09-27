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

package com.microsoft.azure.cosmosserverlessspark.spark.ui.livy.batch

import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobTableModel
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.LivyBatchJobViewer
import com.microsoft.azure.hdinsight.spark.ui.livy.batch.UniqueColumnNameTableSchema
import com.microsoft.azuretools.telemetry.TelemetryConstants
import com.microsoft.azuretools.telemetrywrapper.ErrorType
import com.microsoft.azuretools.telemetrywrapper.EventType
import com.microsoft.azuretools.telemetrywrapper.EventUtil
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable

class CosmosServerlessSparkBatchJobsViewerControl(private val view: CosmosServerlessSparkBatchJobsViewer) : LivyBatchJobViewer.Control, ILogger {
    override fun onNextPage(nextPageLink: String?): LivyBatchJobTableModel.JobPage? {
        log().warn("Trying to get next page which is not supported for now.")
        return null
    }

    override fun onJobSelected(jobSelected: UniqueColumnNameTableSchema.RowDescriptor?) {
        val jobDesc = (jobSelected as? CosmosServerlessSparkBatchJobsTableSchema.CosmosServerlessSparkJobDescriptor)?.let { arrayOf(it) }
            ?: emptyArray()

        val operation = TelemetryManager.createOperation(TelemetryConstants.SPARK_ON_COSMOS_SERVERLESS, TelemetryConstants.SELECT_JOB_IN_JOB_VIEW_WINDOW)
        operation.start()
        Observable.from(jobDesc)
            .flatMap { view.account.getSparkBatchJobWithRawHttpResponse(it[jobUuidColName].toString()) }
            .doOnNext {
                view.setData(
                    view.getModel(LivyBatchJobViewer.Model::class.java).apply {
                        jobDetail = it.message
                    })
            }
            .subscribe(
                    {},
                    { err ->
                        log().warn("Get Cosmos Serverless Spark batch job detail failed. " + ExceptionUtils.getStackTrace(err))
                        EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.serviceError, err, mapOf
                        ("isGetJobDetailsSucceed" to "false"), null)
                    },
                    { EventUtil.logEventWithComplete(EventType.info, operation, mapOf("isGetJobDetailsSucceed" to "true"), null) }
            )
    }
}