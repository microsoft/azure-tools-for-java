/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azuretools.core.mvp.ui.base.MvpView;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisCacheProperty;

public interface RedisPropertyMvpView extends MvpView {

    void onReadProperty(String sid, String id);

    void showProperty(RedisCacheProperty property);
}
