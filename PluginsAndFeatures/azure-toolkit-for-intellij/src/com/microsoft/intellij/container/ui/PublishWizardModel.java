package com.microsoft.intellij.container.ui;

import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;

/**
 * Created by yanzh on 7/11/2017.
 */
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
