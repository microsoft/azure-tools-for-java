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
package com.microsoft.azure.hdinsight.common;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.HDINSIGHT;

import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.IToolWindowProcessor;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SparkSubmissionToolWindowProcessor implements IToolWindowProcessor {

    private static final String yarnRunningUIUrlFormat = "%s/yarnui/hn/proxy/%s/";
    private static final String yarnRunningUIEmulatorUrlFormat = "%s/api/v1/applications/%s/";

    private final JEditorPane jEditorPanel = new JEditorPane();
    private JButton stopButton;
    private JButton openSparkUIButton;

    private String fontFace;
    private final List<IHtmlElement> cachedInfo = new ArrayList<IHtmlElement>();
    private String toolWindowText;

    private PropertyChangeSupport changeSupport;
    private ToolWindow toolWindow;

    private IClusterDetail clusterDetail;
    private int batchId;

    public SparkSubmissionToolWindowProcessor(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
    }

    public void initialize() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        
        UISettings.getInstance().addUISettingsListener(new UISettingsListener() {
            @Override
            public void uiSettingsChanged(UISettings uiSettings) {
                synchronized (this) {
                    for (IHtmlElement htmlElement : cachedInfo) {
                        htmlElement.ChangeTheme();
                    }

                    setToolWindowText(parserHtmlElementList(cachedInfo));
                }

            }
        }, ApplicationManager.getApplication());

        fontFace = jEditorPanel.getFont().getFamily();

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new GridBagLayout());

        jEditorPanel.setMargin(new Insets(0, 10, 0, 0));
        JBScrollPane scrollPane = new JBScrollPane(jEditorPanel);

        stopButton = new JButton(PluginUtil.getIcon(CommonConst.StopIconPath));
        stopButton.setDisabledIcon(PluginUtil.getIcon(CommonConst.StopDisableIconPath));
        stopButton.setEnabled(false);
        stopButton.setToolTipText("stop execution of current application");
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        if (clusterDetail != null) {
                            AppInsightsClient.create(HDInsightBundle.message("SparkSubmissionStopButtionClickEvent"), null);
                            EventUtil.logEvent(EventType.info, HDINSIGHT,
                                HDInsightBundle.message("SparkSubmissionStopButtionClickEvent"), null);
                            try {
                                String livyUrl = clusterDetail instanceof LivyCluster ? ((LivyCluster) clusterDetail).getLivyBatchUrl() : null;
                                HttpResponse deleteResponse = SparkBatchSubmission.getInstance().killBatchJob(livyUrl, batchId);
                                if (deleteResponse.getCode() == 201 || deleteResponse.getCode() == 200) {
                                    jobStatusManager.setJobKilled();
                                    setInfo("========================Stop application successfully=======================");
                                } else {
                                    setError(String.format("Error : Failed to stop spark application. error code : %d, reason :  %s.", deleteResponse.getCode(), deleteResponse.getContent()));
                                }
                            } catch (IOException exception) {
                                setError("Error : Failed to stop spark application. exception : " + exception.toString());
                            }
                        }
                    }
                });
            }
        });


        openSparkUIButton = new JButton(
                PluginUtil.getIcon(IconPathBuilder
                        .custom(CommonConst.OpenSparkUIIconName)
                        .build()));
        openSparkUIButton.setDisabledIcon(PluginUtil.getIcon(CommonConst.OpenSparkUIDisableIconPath));
        openSparkUIButton.setEnabled(false);
        openSparkUIButton.setToolTipText("open the corresponding Spark UI page");
        openSparkUIButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        if(jobStatusManager.isApplicationGenerated()){
                            String connectionURL = clusterDetail.getConnectionUrl();
                            String sparkApplicationUrl = clusterDetail.isEmulator() ?
                                    String.format(yarnRunningUIEmulatorUrlFormat, ((EmulatorClusterDetail)clusterDetail).getSparkHistoryEndpoint(), jobStatusManager.getApplicationId()):
                                    String.format(yarnRunningUIUrlFormat, connectionURL, jobStatusManager.getApplicationId());
                            Desktop.getDesktop().browse(new URI(sparkApplicationUrl));
                        }

                    } catch (Exception browseException) {
                        DefaultLoader.getUIHelper().showError("Failed to browse spark application yarn url", "Spark Submission");
                    }
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        buttonPanel.add(stopButton);
        buttonPanel.add(openSparkUIButton);

        GridBagConstraints c00 = new GridBagConstraints();
        c00.fill = GridBagConstraints.VERTICAL;
        c00.weighty = 1;
        c00.gridx = 0;
        c00.gridy = 0;
        jPanel.add(buttonPanel, c00);

        GridBagConstraints c10 = new GridBagConstraints();
        c10.fill = GridBagConstraints.BOTH;
        c10.weightx = 1;
        c10.weighty = 1;
        c10.gridx = 1;
        c10.gridy = 0;
        jPanel.add(scrollPane, c10);

        toolWindow.getComponent().add(jPanel);
        jEditorPanel.setEditable(false);
        jEditorPanel.setOpaque(false);
        jEditorPanel.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));

        jEditorPanel.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {

                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            String protocol = e.getURL().getProtocol();
                            if(protocol.equals("https") || protocol.equals("http")) {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } else if (protocol.equals("file")) {
                                String path = e.getURL().getFile();
                                File localFile = new File(path);
                                File parentFile = localFile.getParentFile();
                                if(parentFile.exists() && parentFile.isDirectory()) {
                                    Desktop.getDesktop().open(parentFile);
                                }
                            }
                        } catch (Exception exception) {
                            DefaultLoader.getUIHelper().showError(exception.getMessage(), "Open Local Folder Error");
                        }
                    }
                }
            }
        });

        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (ApplicationManager.getApplication().isDispatchThread()) {
                    changeSupportHandler(evt);

                } else {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                                                         @Override
                                                         public void run() {
                                                             changeSupportHandler(evt);
                                                         }
                                                     }
                        );
                    } catch (InterruptedException ignore) {
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void changeSupportHandler(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("toolWindowText")) {
                    jEditorPanel.setText(evt.getNewValue().toString());
                } else if (evt.getPropertyName().equals("isStopButtonEnable")) {
                    stopButton.setEnabled(Boolean.parseBoolean(evt.getNewValue().toString()));
                } else if (evt.getPropertyName().equals("isBrowserButtonEnable")) {
                    openSparkUIButton.setEnabled(Boolean.parseBoolean(evt.getNewValue().toString()));
                }
            }
        };

        jEditorPanel.addPropertyChangeListener(propertyChangeListener);
        changeSupport = new PropertyChangeSupport(jEditorPanel);
        changeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    private JobStatusManager jobStatusManager = new JobStatusManager();

    public JobStatusManager getJobStatusManager() {
        return jobStatusManager;
    }

    public synchronized void setSparkApplicationStopInfo(IClusterDetail clusterDetail, int batchId) {
        this.clusterDetail = clusterDetail;
        this.batchId = batchId;
    }

    public synchronized void setHyperlink(String hyperlinkUrl, String anchorText) {
        cachedInfo.add(new HyperLinkElement(fontFace, DarkThemeManager.getInstance().getInfoColor(), DarkThemeManager.getInstance().getHyperLinkColor(), "", hyperlinkUrl, anchorText));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setHyperLinkWithText(String text, String hyperlinkUrl, String anchorText) {
        cachedInfo.add(new HyperLinkElement(fontFace, DarkThemeManager.getInstance().getInfoColor(), DarkThemeManager.getInstance().getHyperLinkColor(), text, hyperlinkUrl, anchorText));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setError(String errorInfo) {
        cachedInfo.add(new TextElement(fontFace, DarkThemeManager.getInstance().getErrorColor(), errorInfo, MessageInfoType.Error));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setWarning(String warningInfo) {
        cachedInfo.add(new TextElement(fontFace, DarkThemeManager.getInstance().getWarningColor(), warningInfo, MessageInfoType.Warning));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setInfo(String info, boolean isCleanable) {

        TextElement element = isCleanable ? new CleanableTextElement(fontFace, DarkThemeManager.getInstance().getInfoColor(), info, MessageInfoType.Info) :
                new TextElement(fontFace, DarkThemeManager.getInstance().getInfoColor(), info, MessageInfoType.Info);

        if(isCleanable) {
            ++cleanableMessageCounter;
            adjustCleanableMessage();
        }

        cachedInfo.add(element);

        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public void setInfo(String info) {
        setInfo(info, false);
    }

    private static final int MAX_CLEANABLE_SIZE = 400;
    private static final int DELETE_SIZE = 100;
    private int cleanableMessageCounter = 0;

    private void adjustCleanableMessage() {
        if (cleanableMessageCounter >= MAX_CLEANABLE_SIZE) {
            int i = 0, deleteMessageCounter = 0;
            while (deleteMessageCounter < DELETE_SIZE && i < cachedInfo.size()) {
                if (cachedInfo.get(i) instanceof CleanableTextElement) {
                    cachedInfo.remove(i);
                    ++deleteMessageCounter;
                    --cleanableMessageCounter;
                } else {
                    ++i;
                }
            }
        }
    }

    public synchronized void clearAll() {
        cachedInfo.clear();
        cleanableMessageCounter = 0;
    }

    private void setToolWindowText(String toolWindowText) {
        changeSupport.firePropertyChange("toolWindowText", this.toolWindowText, toolWindowText);
        this.toolWindowText = toolWindowText;
    }

    public synchronized void setStopButtonState(Boolean newState) {
        Boolean oldState = stopButton.isEnabled();
        changeSupport.firePropertyChange("isStopButtonEnable", oldState, newState);
    }

    public synchronized void setBrowserButtonState(Boolean newState) {
        Boolean oldState = openSparkUIButton.isEnabled();
        changeSupport.firePropertyChange("isBrowserButtonEnable", oldState, newState);
    }

    private String parserHtmlElementList(List<IHtmlElement> htmlElements) {
        StringBuilder builder = new StringBuilder();
        for (IHtmlElement e : htmlElements) {
            builder.append(e.getHtmlString());
        }

        return builder.toString();
    }

    interface IHtmlElement {
        String getHtmlString();

        void ChangeTheme();
    }

    class TextElement implements IHtmlElement {
        private String fontFace;
        private String fontColor;
        private MessageInfoType messageInfoType;
        private String text;

        public TextElement(String fontFace, String fontColor, String text, MessageInfoType messageInfoType) {
            this.fontFace = fontFace;
            this.fontColor = fontColor;
            this.text = text;
            this.messageInfoType = messageInfoType;
        }

        @Override
        public String getHtmlString() {
            return String.format("<font color=\"%s\" face=\"%s\">%s</font><br />", fontColor, fontFace, text);
        }

        @Override
        public void ChangeTheme() {
            if (messageInfoType == MessageInfoType.Info) {
                this.fontColor = DarkThemeManager.getInstance().getInfoColor();
            } else if (messageInfoType == MessageInfoType.Error) {
                this.fontColor = DarkThemeManager.getInstance().getErrorColor();
            } else if (messageInfoType == MessageInfoType.Warning) {
                this.fontColor = DarkThemeManager.getInstance().getWarningColor();
            }
        }
    }

    class CleanableTextElement extends TextElement {
        public CleanableTextElement(String fontFace, String fontColor, String text, MessageInfoType messageInfoType) {
            super(fontFace, fontColor, text, messageInfoType);
        }
    }

    class HyperLinkElement implements IHtmlElement {
        private String fontFace;
        private String fontColor;
        private String hyperLinkColor;
        private String text;
        private String hyperlinkUrl;
        private String anchorText;

        public HyperLinkElement(String fontFace, String fontColor, String hyperLinkColor, String text, String hyperlinkUrl, String anchorText) {
            this.fontFace = fontFace;
            this.fontColor = fontColor;
            this.hyperLinkColor = hyperLinkColor;
            this.text = text;
            this.hyperlinkUrl = hyperlinkUrl;
            this.anchorText = anchorText;
        }

        @Override
        public String getHtmlString() {
            return String.format("<font color=\"%s\" face=\"%s\">%s</font><a href=\"%s\"><font color=\"%s\" face=\"%s\">%s</font></a><br />",
                    fontColor, fontFace, text, hyperlinkUrl, hyperLinkColor, fontFace, anchorText);
        }

        @Override
        public void ChangeTheme() {
            this.hyperLinkColor = DarkThemeManager.getInstance().getHyperLinkColor();
        }
    }
}
