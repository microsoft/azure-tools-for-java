/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
