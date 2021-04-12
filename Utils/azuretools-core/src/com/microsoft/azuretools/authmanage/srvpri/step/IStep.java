/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.step;

import java.io.IOException;
import java.util.Map;

/**
 * Created by shch on 8/20/2016.
 */
public interface IStep {
    void execute(Map<String, Object> params) throws IOException, InterruptedException;
    void rollback(Map<String, Object> params) throws IOException;
    String getName();
}
