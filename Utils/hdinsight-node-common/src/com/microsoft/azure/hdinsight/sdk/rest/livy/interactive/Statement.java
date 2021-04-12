/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * A statement represents the result of an execution statement.
 *
 * Based on Apache Livy, v0.4.0-incubating, refer to http://livy.incubator.apache.org./docs/0.4.0-incubating/rest-api.html
 */

public class Statement implements IConvertible {
    private int             id;         // The statement id
    private String          code;       // The execution code
    private StatementState  state;      // The execution state
    private StatementOutput output;     // The execution output
    private double          progress;   // The execution progress

    public int getId() {
        return this.id;
    }

    public String getCode() {
        return this.code;
    }

    public StatementState getState() {
        return state;
    }

    public StatementOutput getOutput() {
        return output;
    }

    public double getProgress() {
        return progress;
    }
}
