/**
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

package com.microsoft.intellij.container.ui;

import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;

public class PublishWizardModel extends WizardModel{
    private static final String TEXT_WINDOW_TITLE = "Publish to Web App on Linux";
    private boolean imagePushed = false;

    private PublishWizardDialog dialog;

    private boolean nextEnabled;
    private boolean previousEnabled;
    private boolean finishEnabled;
    private boolean cancelEnabled;

    public PublishWizardModel() {
        super(TEXT_WINDOW_TITLE);
        this.add(new StepOnePage(this));
        this.add(new StepTwoPage(this));
    }

    public boolean isImagePushed() {
        return imagePushed;
    }

    public void setImagePushed(boolean imagePushed) {
        this.imagePushed = imagePushed;
    }

    public PublishWizardDialog getDialog() {
        return dialog;
    }

    public void setDialog(PublishWizardDialog dialog) {
        this.dialog = dialog;
    }

    public void setAndCacheNextEnabled(boolean nextEnabled) {
        this.nextEnabled = nextEnabled;
        getCurrentNavigationState().NEXT.setEnabled(nextEnabled);
    }

    public void setAndCachePreviousEnabled(boolean previousEnabled) {
        this.previousEnabled = previousEnabled;
        getCurrentNavigationState().PREVIOUS.setEnabled(previousEnabled);
    }

    public void setAndCacheFinishEnabled(boolean finishEnabled) {
        this.finishEnabled = finishEnabled;
        getCurrentNavigationState().FINISH.setEnabled(finishEnabled);
    }

    public void setAndCacheCancelEnabled(boolean cancelEnabled) {
        this.cancelEnabled = cancelEnabled;
        getCurrentNavigationState().CANCEL.setEnabled(cancelEnabled);
    }
    public void setAndCacheAllEnabled(boolean enabled) {
        setAndCacheNextEnabled(enabled);
        setAndCachePreviousEnabled(enabled);
        setAndCacheFinishEnabled(enabled);
        setAndCacheCancelEnabled(enabled);
    }
    public void loadCachedButtonEnabledStatus() {
        WizardNavigationState state = getCurrentNavigationState();
        state.NEXT.setEnabled(nextEnabled);
        state.PREVIOUS.setEnabled(previousEnabled);
        state.FINISH.setEnabled(finishEnabled);
        state.CANCEL.setEnabled(cancelEnabled);
    }

    public void cacheButtonEnabledStatus() {
        WizardNavigationState state = getCurrentNavigationState();
        nextEnabled = state.NEXT.isEnabled();
        previousEnabled = state.PREVIOUS.isEnabled();
        finishEnabled = state.FINISH.isEnabled();
        cancelEnabled = state.CANCEL.isEnabled();
    }

    public void resetDefaultButtonEnabledStatus() {
        WizardNavigationState state = getCurrentNavigationState();
        WizardStep myCurrentStep = getCurrentStep();
        state.NEXT.setEnabled(!isLast(myCurrentStep));
        state.PREVIOUS.setEnabled(!isFirst(myCurrentStep));
        state.FINISH.setEnabled(isLast(myCurrentStep));
        state.CANCEL.setEnabled(true);
    }
}
