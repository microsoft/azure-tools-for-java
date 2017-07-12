package com.microsoft.intellij.container.ui;

import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.intellij.container.mvp.PublishWizardPageView;
import com.microsoft.intellij.container.mvp.StepOnePagePresenter;
import com.microsoft.intellij.container.mvp.StepOnePageView;
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification;
import com.microsoft.intellij.ui.components.AzureWizardStep;

import javax.swing.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yanzh on 7/11/2017.
 */
public class StepOnePage extends AzureWizardStep<PublishWizardModel> implements StepOnePageView, PublishWizardPageView {
    private static final String TEXT_TITLE = "Push Docker Image to Azure Container Registry";
    private static final String TEXT_DESCRIPTION = "Complete the credential of your Azure Container Registry";
    private JTextField txtRegistryUrl;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextArea txtInformation;
    private JPanel rootPanel;
    private final StepOnePagePresenter<StepOnePage> presenter;
    private boolean validated = false;
    private PublishWizardModel model;

    public StepOnePage(PublishWizardModel publishWizardModel) {
        super(TEXT_TITLE, TEXT_DESCRIPTION);
        model = publishWizardModel;
        presenter = new StepOnePagePresenter<StepOnePage>();
        presenter.onAttachView(this);
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        initilize();
        return rootPanel;
    }

    @Override
    public WizardStep onNext(PublishWizardModel model) {
        if(validated) {
            return super.onNext(model);
        }
        else{
            onWizardNextPressed();
            return this;
        }
    }

    @Override
    public void onWizardNextPressed() {

        presenter.onPushLatestImageToRegistry(txtRegistryUrl.getText(), txtUsername.getText(), new String(txtPassword.getPassword()));
    }

    @Override
    public void onWizardFinishPressed() {
        return;
    }


    @Override
    public void fillRegistryInfo(String registryUrl, String username, String password) {
        txtRegistryUrl.setText(registryUrl);
        txtUsername.setText(username);
        txtPassword.setText(password);
    }


    @Override
    public void onRequestPending(Object payload) {
        //TODO
        showInformation("Try pushing image ...");
        model.getDialog().getNextButton().setEnabled(false);
        System.out.println(payload);
    }

    @Override
    public void onRequestSucceed(Object payload) {
        //TODO
        showInformation("Task OK, please click `Next` again to continue.");
        model.getDialog().getNextButton().setEnabled(true);
        validated = true;
        System.out.println(payload);
    }

    @Override
    public void onRequestFail(Object payload) {
        //TODO
        showInformation("Task FAIL");
        model.getDialog().getNextButton().setEnabled(true);
        validated = false;
        System.out.println(payload);
    }
    // TODO: dispose detach presenter

    private void initilize() {
        presenter.onLoadRegistryInfo();
    }

    private void showInformation(String string) {
        if (string == null) {
            return;
        }
        DateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        String date = df.format(new Date());
        txtInformation.append(String.format("[%s]\t%s\n", date, string));
    }
}
