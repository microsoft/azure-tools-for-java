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

package com.microsoft.azuretools.authmanage.srvpri.step;

import com.microsoft.azuretools.authmanage.srvpri.report.Reporter;

import java.util.List;

/**
 * Created by vlashch on 10/18/16.
 */
public class CommonParams {

    private static String tenantId;
    private static List<String> subscriptionIdList;
    private static List<String> resultSubscriptionIdList;
//    private static List<Status> statusList = new ArrayList<>();
    private static Reporter<String> reporter;
    private static Reporter<Status> statusReporter;

    public static Reporter<Status> getStatusReporter() {
        return statusReporter;
    }

    public static void setStatusReporter(Reporter<Status> statusReporter) {
        CommonParams.statusReporter = statusReporter;
    }

    public static List<String> getResultSubscriptionIdList() {
        return resultSubscriptionIdList;
    }

    public static void setResultSubscriptionIdList(List<String> resultSubscriptionIdList) {
        CommonParams.resultSubscriptionIdList = resultSubscriptionIdList;
    }

    public static Reporter<String> getReporter() {
        return reporter;
    }

    public static void setReporter(Reporter<String> reporter) {
        CommonParams.reporter = reporter;
    }

    public static String getTenantId() {
        return tenantId;
    }

    public static void setTenantId(String tenantId) {
        CommonParams.tenantId = tenantId;
    }

    public static List<String> getSubscriptionIdList() {
        return subscriptionIdList;
    }

    public static void setSubscriptionIdList(List<String> subscriptionIdList) {
        CommonParams.subscriptionIdList = subscriptionIdList;
    }
}
