/*
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.azure.hdinsight.spark.common;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public interface ISparkBatchJob {
    /**
     * Getter of Spark Batch Job submission parameter
     *
     * @return the instance of Spark Batch Job submission parameter
     */
    public SparkSubmissionParameter getSubmissionParameter();

    /**
     * Getter of the Spark Batch Job submission for RestAPI transaction
     *
     * @return the Spark Batch Job submission
     */
    public SparkBatchSubmission getSubmission();

    /**
     * Getter of the base connection URI for HDInsight Spark Job service
     *
     * @return the base connection URI for HDInsight Spark Job service
     */
    public URI getConnectUri();

    /**
     * Getter of the LIVY Spark batch job ID got from job submission
     *
     * @return the LIVY Spark batch job ID
     */
    public int getBatchId();

    /**
     * Getter of the maximum retry count in RestAPI calling
     *
     * @return the maximum retry count in RestAPI calling
     */
    public int getRetriesMax();

    /**
     * Setter of the maximum retry count in RestAPI calling
     * @param retriesMax the maximum retry count in RestAPI calling
     */
    public void setRetriesMax(int retriesMax);

    /**
     * Getter of the delay seconds between tries in RestAPI calling
     *
     * @return the delay seconds between tries in RestAPI calling
     */
    public int getDelaySeconds();

    /**
     * Setter of the delay seconds between tries in RestAPI calling
     * @param delaySeconds the delay seconds between tries in RestAPI calling
     */
    public void setDelaySeconds(int delaySeconds);

    /**
     * Create a batch Spark job with driver debugging enabled
     *
     * @return the current instance for chain calling
     * @throws IOException the exceptions for networking connection issues related
     */
    public ISparkBatchJob createBatchJob() throws IOException;

    /**
     * Kill the batch job specified by ID
     *
     * @return the current instance for chain calling
     * @throws IOException exceptions for networking connection issues related
     */
    public ISparkBatchJob killBatchJob() throws IOException;

    /**
     * Get Spark batch job driver host by ID
     *
     * @return Spark driver node host
     * @throws IOException exceptions for the driver host not found
     */
    public String getSparkDriverHost() throws IOException, URISyntaxException;
}
