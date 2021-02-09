/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import java.util.ArrayList;
import java.util.List;

public class AggregatedException extends Exception{
    private List<Exception> exceptionList = new ArrayList<>();
    public AggregatedException(List<Exception> exceptionList){
        this.exceptionList = exceptionList;
    }

    public List<Exception> getExceptionList(){
        return exceptionList;
    }
}
