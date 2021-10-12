/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.DataLakeAnalyticsAccountBasic;
import com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models.PageImpl;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAccountsListResponse extends PageImpl<DataLakeAnalyticsAccountBasic>
        implements IConvertible {

}
