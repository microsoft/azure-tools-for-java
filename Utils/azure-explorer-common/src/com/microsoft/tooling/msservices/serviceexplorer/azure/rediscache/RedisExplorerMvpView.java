/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azuretools.core.mvp.ui.base.MvpView;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisScanResult;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisValueData;

public interface RedisExplorerMvpView extends MvpView {

    void renderDbCombo(int num);

    void showScanResult(RedisScanResult result);

    void showContent(RedisValueData val);

    void updateKeyList();

    void getKeyFail();
}
