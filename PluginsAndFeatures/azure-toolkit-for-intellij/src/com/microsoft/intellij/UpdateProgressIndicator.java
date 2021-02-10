/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.azuretools.utils.IProgressIndicator;

/**
 * Created by vlashch on 1/20/17.
 */
public class UpdateProgressIndicator implements IProgressIndicator {
    private ProgressIndicator progressIndicator;

    public UpdateProgressIndicator(ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }
    @Override
    public void setText(String s) {
        progressIndicator.setText(s);
    }

    @Override
    public void setText2(String s) {
        progressIndicator.setText2(s);

    }

    @Override
    public void setFraction(double v) {
        progressIndicator.setFraction(v);
    }

    @Override
    public boolean isCanceled() {
        return progressIndicator.isCanceled();
    }

    @Override
    public void notifyComplete() {}
}
