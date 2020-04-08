/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.hdinsight;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.HDINSIGHT;

import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.JobStatusManager;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.core.utils.Messages;

public class SparkSubmissionToolWindowView extends ViewPart {
    private static final String yarnRunningUIUrlFormat = "%s/yarnui/hn/proxy/%s/";
    
    private Button stopButton;
    private Button openSparkUIButton;
    private Browser outputPanel;
    private final List<IHtmlElement> cachedInfo = new ArrayList<IHtmlElement>();
    private String toolWindowText;

    private PropertyChangeSupport changeSupport;
    private JobStatusManager jobStatusManager = new JobStatusManager();
    
    private String connectionUrl;
    private int batchId;
    
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		parent.setLayout(layout);
		
		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		composite.setLayout(layout);
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		composite.setLayoutData(gridData);
		stopButton = new Button(composite, SWT.PUSH);
		stopButton.setText("Stop");
        stopButton.setToolTipText("Stop execution of current application");
        stopButton.setImage(Activator.getImageDescriptor(CommonConst.StopIconPath).createImage());
        stopButton.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent evt) {
        		DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!StringHelper.isNullOrWhiteSpace(connectionUrl)) {
                        	AppInsightsClient.create(Messages.SparkSubmissionStopButtionClickEvent, null);
                            EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkSubmissionStopButtionClickEvent, null);
                            try {
                                HttpResponse deleteResponse = SparkBatchSubmission.getInstance().killBatchJob(connectionUrl + "/livy/batches", batchId);
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
        stopButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
        	public void getName(AccessibleEvent e)  {
        		super.getName(e);
        		e.result = stopButton.getToolTipText();
        	}
        });
        
		openSparkUIButton = new Button(composite, SWT.PUSH);
		openSparkUIButton.setText("Open Spark UI");
		openSparkUIButton.setToolTipText("Open the corresponding Spark UI page");
		openSparkUIButton.setImage(Activator.getImageDescriptor(CommonConst.OpenSparkUIIconPath).createImage());
		openSparkUIButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				try {
					if (jobStatusManager.isApplicationGenerated()) {
						String sparkApplicationUrl = String.format(yarnRunningUIUrlFormat, connectionUrl, jobStatusManager.getApplicationId());
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(sparkApplicationUrl));
					}
				} catch (Exception browseException) {
					DefaultLoader.getUIHelper().showError("Failed to browse spark application yarn url", "Spark Submission");
				}
			}
        });
		openSparkUIButton.getAccessible().addAccessibleListener(new AccessibleAdapter() {
        	public void getName(AccessibleEvent e)  {
        		super.getName(e);
        		e.result = openSparkUIButton.getToolTipText();
        	}
        });
		
		gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessVerticalSpace = true;
        gridData.grabExcessHorizontalSpace = true;
        outputPanel = new Browser(parent, SWT.BORDER);
        outputPanel.setLayoutData(gridData);
        
        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
//                if (ApplicationManager.getApplication().isDispatchThread()) {
                    changeSupportHandler(evt);

////                } else {
//                    try {
//                        SwingUtilities.invokeAndWait(new Runnable() {
//                                                         @Override
//                                                         public void run() {
//                                                             changeSupportHandler(evt);
//                                                         }
//                                                     }
//                        );
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    } catch (InvocationTargetException e) {
//                        e.printStackTrace();
//                    }
//                }
            }

            private void changeSupportHandler(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("toolWindowText")) {
                    outputPanel.setText(evt.getNewValue().toString());
                } else if (evt.getPropertyName().equals("isStopButtonEnable")) {
                    stopButton.setEnabled(Boolean.parseBoolean(evt.getNewValue().toString()));
                } else if (evt.getPropertyName().equals("isBrowserButtonEnable")) {
                    openSparkUIButton.setEnabled(Boolean.parseBoolean(evt.getNewValue().toString()));
                }
            }
        };

//        outputPanel.addPropertyChangeListener(propertyChangeListener);
        changeSupport = new PropertyChangeSupport(outputPanel);
        changeSupport.addPropertyChangeListener(propertyChangeListener);
	}    

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	public JobStatusManager getJobStatusManager() {
	    return jobStatusManager;
	}
	
	public synchronized void setSparkApplicationStopInfo(String connectUrl, int batchId) {
        this.connectionUrl = connectUrl;
        this.batchId = batchId;
    }
	
	public synchronized void setHyperLinkWithText(String text, String hyperlinkUrl, String anchorText) {
        cachedInfo.add(new HyperLinkElement(/*fontFace, DarkThemeManager.getInstance().getInfoColor(), DarkThemeManager.getInstance().getHyperLinkColor(), */text, hyperlinkUrl, anchorText));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setError(String errorInfo) {
        cachedInfo.add(new TextElement(/*fontFace, DarkThemeManager.getInstance().getErrorColor(), */errorInfo, MessageInfoType.Error));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setWarning(String warningInfo) {
        cachedInfo.add(new TextElement(/*fontFace, DarkThemeManager.getInstance().getWarningColor(), */warningInfo, MessageInfoType.Warning));
        setToolWindowText(parserHtmlElementList(cachedInfo));
    }

    public synchronized void setInfo(String info, boolean isCleanable) {

        TextElement element = isCleanable ? new CleanableTextElement(/*fontFace, DarkThemeManager.getInstance().getInfoColor(), */info, MessageInfoType.Info) :
                new TextElement(/*fontFace, DarkThemeManager.getInstance().getInfoColor(), */info, MessageInfoType.Info);

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

	private void setToolWindowText(final String toolWindowText) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				changeSupport.firePropertyChange("toolWindowText", SparkSubmissionToolWindowView.this.toolWindowText, toolWindowText);			
			}
		});
		SparkSubmissionToolWindowView.this.toolWindowText = toolWindowText;
    }

	public synchronized void setStopButtonState(final Boolean newState) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Boolean oldState = stopButton.isEnabled();
				changeSupport.firePropertyChange("isStopButtonEnable", oldState, newState);
			}
		});
	}

	public synchronized void setBrowserButtonState(final Boolean newState) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Boolean oldState = openSparkUIButton.isEnabled();
				changeSupport.firePropertyChange("isBrowserButtonEnable", oldState, newState);
			}
		});
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

        public TextElement(/*String fontFace, String fontColor,*/ String text, MessageInfoType messageInfoType) {
//            this.fontFace = fontFace;
//            this.fontColor = fontColor;
            this.text = text;
            this.messageInfoType = messageInfoType;
        }

        @Override
        public String getHtmlString() {
            return String.format("<font color=\"%s\" face=\"%s\">%s</font><br />", fontColor, fontFace, text);
        }

        @Override
        public void ChangeTheme() {
//            if (messageInfoType == MessageInfoType.Info) {
//                this.fontColor = DarkThemeManager.getInstance().getInfoColor();
//            } else if (messageInfoType == MessageInfoType.Error) {
//                this.fontColor = DarkThemeManager.getInstance().getErrorColor();
//            } else if (messageInfoType == MessageInfoType.Warning) {
//                this.fontColor = DarkThemeManager.getInstance().getWarningColor();
//            }
        }
    }

    class CleanableTextElement extends TextElement {
        public CleanableTextElement(/*String fontFace, String fontColor, */String text, MessageInfoType messageInfoType) {
            super(/*fontFace, fontColor,*/ text, messageInfoType);
        }
    }

    class HyperLinkElement implements IHtmlElement {
        private String fontFace;
        private String fontColor;
        private String hyperLinkColor;
        private String text;
        private String hyperlinkUrl;
        private String anchorText;

        public HyperLinkElement(/*String fontFace, String fontColor, String hyperLinkColor, */String text, String hyperlinkUrl, String anchorText) {
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
//            this.hyperLinkColor = DarkThemeManager.getInstance().getHyperLinkColor();
        }
    }

}
