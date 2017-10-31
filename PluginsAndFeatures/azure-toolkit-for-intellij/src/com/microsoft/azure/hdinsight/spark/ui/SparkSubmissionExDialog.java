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
package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitHelper;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;

public class SparkSubmissionExDialog extends JDialog {
    private SparkSubmissionContentPanelConfigurable contentControl;

    private final int margin = 10;
    private final String DialogTitle = "Spark Submission";

    private JButton buttonSubmit;

    private Project project;
    private SparkSubmitModel submitModel;
    private CallBack callBack;

    public SparkSubmissionExDialog(@NotNull Project project, @Nullable CallBack callBack) {
        this.project = project;
        this.callBack = callBack;
        submitModel = new SparkSubmitModel(project);

        initializeComponents();
        setSubmitButtonStatus();
        this.pack();
    }

    //region UI Constructor
    private void initializeComponents() {
        Image image = StreamUtil.getImageResourceFile(CommonConst.ProductIConPath).getImage();
        setIconImage(image);

        contentControl = new SparkSubmissionContentPanelConfigurable(this.project, () -> {
            setSubmitButtonStatus();
            pack();
        });
        setContentPane(getContentPane());

        setModal(true);
        setTitle(DialogTitle);

        addOperationJPanel();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        getContentPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    @Override
    public SparkSubmissionContentPanel getContentPane() {
        return (SparkSubmissionContentPanel) contentControl.getComponent();
    }

    private void addOperationJPanel() {
        JPanel operationPanel = new JPanel();
        operationPanel.setLayout(new FlowLayout());
        buttonSubmit = new JButton("Submit");
        JButton buttonCancel = new JButton("Cancel");
        JButton buttonHelper = new JButton("Help >>");

        operationPanel.add(buttonSubmit);
        operationPanel.add(buttonCancel);
        operationPanel.add(buttonHelper);

        getContentPane().add(operationPanel,
                new GridBagConstraints(1, ++(getContentPane().displayLayoutCurrentRow),
                        0, 1,
                        1, 0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE,
                        new Insets(0, margin, 0, 0), 0, 0));

        buttonSubmit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        buttonHelper.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OnHelper();
            }
        });
    }

    private void onOK() {
        contentControl.getData(submitModel);
        submitModel.action(submitModel.getSubmissionParameter());
        dispose();
    }
    //endregion

    private void setSubmitButtonStatus() {
        if (this.getContentPane() == null || this.getContentPane().haveErrorMessage()) {
            if (this.buttonSubmit != null){
                this.buttonSubmit.setEnabled(false);
            }
        }
        else{
            if (this.buttonSubmit != null) {
                this.buttonSubmit.setEnabled(true);
            }
        }
    }

    private void onCancel() {
        dispose();
    }

    private void OnHelper() {
        try {
            AppInsightsClient.create(HDInsightBundle.message("SparkSubmissionHelpClickEvent"), null);
            Desktop.getDesktop().browse(new URI(SparkSubmitHelper.HELP_LINK));
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showException("An error occurred while attempting to browse link.", e,
                    "HDInsight Spark Submit - Error Browsing Link", false, true);
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        if (this.callBack != null) {
            callBack.run();
        }
    }
}
