/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.common;

import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
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
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.HDINSIGHT;

public class SparkSubmissionToolWindowProcessor implements IToolWindowProcessor {

    private static final String yarnRunningUIUrlFormat = "%s/yarnui/hn/proxy/%s/";
    private static final String yarnRunningUIEmulatorUrlFormat = "%s/api/v1/applications/%s/";

    private final JEditorPane jEditorPanel = new JEditorPane();
    private JButton stopButton;
    private JButton openSparkUIButton;

    private String fontFace;
    private final List<IHtmlElement> cachedInfo = new ArrayList<>();
    private String toolWindowText;

    private PropertyChangeSupport changeSupport;
    private final ToolWindow toolWindow;

    private IClusterDetail clusterDetail;
    private int batchId;

    public SparkSubmissionToolWindowProcessor(final ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
    }

    public void initialize() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        // TODO: Fix deprecated API "addUISettingsListener"
        UISettings.getInstance().addUISettingsListener(new UISettingsListener() {
            @Override
            public void uiSettingsChanged(final UISettings uiSettings) {
                synchronized (this) {
                    for (final IHtmlElement htmlElement : cachedInfo) {
                        htmlElement.changeTheme();
                    }

                    setToolWindowText(parserHtmlElementList(cachedInfo));
                }

            }
        }, ApplicationManager.getApplication());

        fontFace = jEditorPanel.getFont().getFamily();

        final JPanel jPanel = new JPanel();
        jPanel.setLayout(new GridBagLayout());

        jEditorPanel.setMargin(JBUI.insetsLeft(10));
        final JBScrollPane scrollPane = new JBScrollPane(jEditorPanel);

        stopButton = new JButton(PluginUtil.getIcon(CommonConst.StopIconPath));
        stopButton.setDisabledIcon(PluginUtil.getIcon(CommonConst.StopDisableIconPath));
        stopButton.setEnabled(false);
        stopButton.setToolTipText("stop execution of current application");
        stopButton.addActionListener(e -> DefaultLoader.getIdeHelper().executeOnPooledThread(() -> {
            if (clusterDetail != null) {
                AppInsightsClient.create(HDInsightBundle.message("SparkSubmissionStopButtionClickEvent"),
                                         null);
                EventUtil.logEvent(EventType.info, HDINSIGHT,
                                   HDInsightBundle.message("SparkSubmissionStopButtionClickEvent"), null);
                try {
                    final String livyUrl = clusterDetail instanceof LivyCluster
                                           ? ((LivyCluster) clusterDetail).getLivyBatchUrl()
                                           : null;
                    final HttpResponse deleteResponse =
                            SparkBatchSubmission.getInstance().killBatchJob(livyUrl, batchId);
                    if (deleteResponse.getCode() == 201 || deleteResponse.getCode() == 200) {
                        jobStatusManager.setJobKilled();
                        setInfo("========================Stop application successfully"
                                        + "=======================");
                    } else {
                        setError(String.format(
                                "Error : Failed to stop spark application. error code : %d, reason :  %s.",
                                deleteResponse.getCode(),
                                deleteResponse.getContent()));
                    }
                } catch (final IOException exception) {
                    setError("Error : Failed to stop spark application. exception : "
                                     + exception.toString());
                }
            }
        }));

        openSparkUIButton = new JButton(
                PluginUtil.getIcon(IconPathBuilder
                                           .custom(CommonConst.OpenSparkUIIconName)
                                           .build()));
        openSparkUIButton.setDisabledIcon(PluginUtil.getIcon(CommonConst.OpenSparkUIDisableIconPath));
        openSparkUIButton.setEnabled(false);
        openSparkUIButton.setToolTipText("open the corresponding Spark UI page");
        openSparkUIButton.addActionListener(e -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    if (jobStatusManager.isApplicationGenerated()) {
                        final String connectionURL = clusterDetail.getConnectionUrl();
                        final String sparkApplicationUrl =
                                clusterDetail.isEmulator()
                                ? String.format(yarnRunningUIEmulatorUrlFormat,
                                                ((EmulatorClusterDetail) clusterDetail).getSparkHistoryEndpoint(),
                                                jobStatusManager.getApplicationId())
                                : String.format(yarnRunningUIUrlFormat,
                                                connectionURL,
                                                jobStatusManager.getApplicationId());
                        Desktop.getDesktop().browse(new URI(sparkApplicationUrl));
                    }

                } catch (final Exception browseException) {
                    DefaultLoader.getUIHelper().showError("Failed to browse spark application yarn url",
                                                          "Spark Submission");
                }
            }
        });

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        buttonPanel.add(stopButton);
        buttonPanel.add(openSparkUIButton);

        final GridBagConstraints c00 = new GridBagConstraints();
        c00.fill = GridBagConstraints.VERTICAL;
        c00.weighty = 1;
        c00.gridx = 0;
        c00.gridy = 0;
        jPanel.add(buttonPanel, c00);

        final GridBagConstraints c10 = new GridBagConstraints();
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

        jEditorPanel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        final String protocol = e.getURL().getProtocol();
                        if ("https".equals(protocol) || "http".equals(protocol)) {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } else if ("file".equals(protocol)) {
                            final String path = e.getURL().getFile();
                            final File localFile = new File(path);
                            final File parentFile = localFile.getParentFile();
                            if (parentFile.exists() && parentFile.isDirectory()) {
                                Desktop.getDesktop().open(parentFile);
                            }
                        }
                    } catch (final Exception exception) {
                        DefaultLoader.getUIHelper().showError(exception.getMessage(), "Open Local Folder Error");
                    }
                }
            }
        });

        final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (ApplicationManager.getApplication().isDispatchThread()) {
                    changeSupportHandler(evt);

                } else {
                    try {
                        SwingUtilities.invokeAndWait(() -> changeSupportHandler(evt));
                    } catch (final InterruptedException ignore) {
                    } catch (final InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void changeSupportHandler(final PropertyChangeEvent evt) {
                if ("toolWindowText".equals(evt.getPropertyName())) {
                    jEditorPanel.setText(evt.getNewValue().toString());
                } else if ("isStopButtonEnable".equals(evt.getPropertyName())) {
                    stopButton.setEnabled(Boolean.parseBoolean(evt.getNewValue().toString()));
                } else if ("isBrowserButtonEnable".equals(evt.getPropertyName())) {
                    openSparkUIButton.setEnabled(Boolean.parseBoolean(evt.getNewValue().toString()));
                }
            }
        };

        jEditorPanel.addPropertyChangeListener(propertyChangeListener);
        changeSupport = new PropertyChangeSupport(jEditorPanel);
        changeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    private final JobStatusManager jobStatusManager = new JobStatusManager();

    public JobStatusManager getJobStatusManager() {
        return jobStatusManager;
    }

    public synchronized void setSparkApplicationStopInfo(final IClusterDetail clusterDetail, final int batchId) {
        this.clusterDetail = clusterDetail;
        this.batchId = batchId;
    }

    public synchronized void setHyperlink(final String hyperlinkUrl, final String anchorText) {
        cachedInfo.add(new HyperLinkElement(fontFace,
                                            DarkThemeManager.getInstance().getInfoColor(),
                                            DarkThemeManager.getInstance().getHyperLinkColor(),
                                            "",
                                            hyperlinkUrl,
                                            anchorText));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setHyperLinkWithText(final String text,
                                                  final String hyperlinkUrl,
                                                  final String anchorText) {
        cachedInfo.add(new HyperLinkElement(fontFace,
                                            DarkThemeManager.getInstance().getInfoColor(),
                                            DarkThemeManager.getInstance().getHyperLinkColor(),
                                            text,
                                            hyperlinkUrl,
                                            anchorText));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setError(final String errorInfo) {
        cachedInfo.add(new TextElement(fontFace,
                                       DarkThemeManager.getInstance().getErrorColor(),
                                       errorInfo,
                                       MessageInfoType.Error));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setWarning(final String warningInfo) {
        cachedInfo.add(new TextElement(fontFace,
                                       DarkThemeManager.getInstance().getWarningColor(),
                                       warningInfo,
                                       MessageInfoType.Warning));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setInfo(final String info, final boolean isCleanable) {

        final TextElement element = isCleanable
                                    ? new CleanableTextElement(fontFace,
                                                               DarkThemeManager.getInstance().getInfoColor(),
                                                               info,
                                                               MessageInfoType.Info)
                                    : new TextElement(fontFace,
                                                      DarkThemeManager.getInstance().getInfoColor(),
                                                      info,
                                                      MessageInfoType.Info);

        if (isCleanable) {
            ++cleanableMessageCounter;
            adjustCleanableMessage();
        }

        cachedInfo.add(element);

        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public void setInfo(final String info) {
        setInfo(info, false);
    }

    private static final int MAX_CLEANABLE_SIZE = 400;
    private static final int DELETE_SIZE = 100;
    private int cleanableMessageCounter = 0;

    private void adjustCleanableMessage() {
        if (cleanableMessageCounter >= MAX_CLEANABLE_SIZE) {
            int i = 0;
            int deleteMessageCounter = 0;
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

    private void setToolWindowText(final String toolWindowText) {
        changeSupport.firePropertyChange("toolWindowText", this.toolWindowText, toolWindowText);
        this.toolWindowText = toolWindowText;
    }

    public synchronized void setStopButtonState(final Boolean newState) {
        final Boolean oldState = stopButton.isEnabled();
        changeSupport.firePropertyChange("isStopButtonEnable", oldState, newState);
    }

    public synchronized void setBrowserButtonState(final Boolean newState) {
        final Boolean oldState = openSparkUIButton.isEnabled();
        changeSupport.firePropertyChange("isBrowserButtonEnable", oldState, newState);
    }

    private String parserHtmlElementList(final List<? extends IHtmlElement> htmlElements) {
        final StringBuilder builder = new StringBuilder();
        for (final IHtmlElement e : htmlElements) {
            builder.append(e.getHtmlString());
        }

        return builder.toString();
    }

    interface IHtmlElement {
        String getHtmlString();

        void changeTheme();
    }

    static class TextElement implements IHtmlElement {
        private final String fontFace;
        private String fontColor;
        private final MessageInfoType messageInfoType;
        private final String text;

        public TextElement(final String fontFace,
                           final String fontColor,
                           final String text,
                           final MessageInfoType messageInfoType) {
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
        public void changeTheme() {
            if (messageInfoType == MessageInfoType.Info) {
                this.fontColor = DarkThemeManager.getInstance().getInfoColor();
            } else if (messageInfoType == MessageInfoType.Error) {
                this.fontColor = DarkThemeManager.getInstance().getErrorColor();
            } else if (messageInfoType == MessageInfoType.Warning) {
                this.fontColor = DarkThemeManager.getInstance().getWarningColor();
            }
        }
    }

    static class CleanableTextElement extends TextElement {
        public CleanableTextElement(final String fontFace,
                                    final String fontColor,
                                    final String text,
                                    final MessageInfoType messageInfoType) {
            super(fontFace, fontColor, text, messageInfoType);
        }
    }

    static class HyperLinkElement implements IHtmlElement {
        private final String fontFace;
        private final String fontColor;
        private String hyperLinkColor;
        private final String text;
        private final String hyperlinkUrl;
        private final String anchorText;

        public HyperLinkElement(final String fontFace,
                                final String fontColor,
                                final String hyperLinkColor,
                                final String text,
                                final String hyperlinkUrl,
                                final String anchorText) {
            this.fontFace = fontFace;
            this.fontColor = fontColor;
            this.hyperLinkColor = hyperLinkColor;
            this.text = text;
            this.hyperlinkUrl = hyperlinkUrl;
            this.anchorText = anchorText;
        }

        @Override
        public String getHtmlString() {
            return String.format(
                    "<font color=\"%s\" face=\"%s\">%s</font><a href=\"%s\">"
                            + "<font color=\"%s\" face=\"%s\">%s</font></a><br />",
                    fontColor,
                    fontFace,
                    text,
                    hyperlinkUrl,
                    hyperLinkColor,
                    fontFace,
                    anchorText);
        }

        @Override
        public void changeTheme() {
            this.hyperLinkColor = DarkThemeManager.getInstance().getHyperLinkColor();
        }
    }
}
