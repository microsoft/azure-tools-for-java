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
package com.microsoft.azuretools.hdinsight.projects;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.projects.SparkVersion;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.hdinsight.Activator;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;

public class SparkLibraryOptionsPanel extends Composite {
    private boolean isUsingMaven = true;
    private SparkVersion sparkVersion;
    private Composite comUseMaven;
    private Composite comAddSparkManually;
    private Button radioUseMaven;
    private Button radioAddManually;
    private Combo comboBoxUseMaven;
    private Combo comboBoxAddSparkManually;
    private Link tipLabel;
    private Button btnSelectSparkLib;
    private WizardPage parentPage;
    private List<String> cachedLibraryPath = new ArrayList<>();

    public SparkLibraryOptionsPanel(WizardPage parentPage,Composite parent, int style) {
        super(parent, style);
        this.parentPage = parentPage;
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        setLayout(gridLayout);
        setLayoutData(gridData);

        // Use Maven to configure Spark version
        radioUseMaven = new Button(this, SWT.RADIO);
        radioUseMaven.setSelection(true);
        radioUseMaven.setText("Use Maven to configure Spark SDK:");
        radioUseMaven.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                enableUseMaven(true);

                enableAddSparkManually(false);

                updateNextPageStatus();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        comUseMaven = new Composite(this, SWT.NONE);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        comUseMaven.setLayout(gridLayout);
        comUseMaven.setLayoutData(gridData);

        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        comboBoxUseMaven = new Combo(comUseMaven, SWT.READ_ONLY);
        comboBoxUseMaven.setLayoutData(gridData);
        for (SparkVersion sv : SparkVersion.class.getEnumConstants()) {
            // Ignore Spark version suports for higher than 2.3.0
            switch (sv) {
            case SPARK_2_4_0:
            case SPARK_2_3_2:
                continue;
            default:
            }
            comboBoxUseMaven.add(sv.toString(), comboBoxUseMaven.getItemCount());
        }
        sparkVersion = SparkVersion.class.getEnumConstants()[0];

        comboBoxUseMaven.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Combo combo = (Combo)e.getSource();
                if (combo.getSelectionIndex() >= 0) {
                    sparkVersion = SparkVersion.parseString(combo.getItem(combo.getSelectionIndex()));
                }

                updateNextPageStatus();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

        });

        comboBoxUseMaven.select(0);
        enableUseMaven(true);

        // Add Spark SDK manually
        radioAddManually = new Button(this, SWT.RADIO);
        radioAddManually.setText("Add Spark SDK manually:");
        radioAddManually.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                enableUseMaven(false);

                enableAddSparkManually(true);

                updateNextPageStatus();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

        });

        gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        comAddSparkManually = new Composite(this, SWT.NONE);
        comAddSparkManually.setLayout(gridLayout);
        comAddSparkManually.setLayoutData(gridData);

        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        comboBoxAddSparkManually = new Combo(comAddSparkManually, SWT.READ_ONLY);
        comboBoxAddSparkManually.setLayoutData(gridData);

        comboBoxAddSparkManually.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Combo combo = (Combo)e.getSource();
                if(combo != null) {
                    int index = combo.getSelectionIndex();
                    if(index != -1) {
                        String selectItem = combo.getItem(index);
                        cachedLibraryPath.remove(selectItem);
                        cachedLibraryPath.add(0, selectItem);
                    } else if(combo.getItemCount() >= 1){
                        combo.select(0);
                    }

                    updateNextPageStatus();
                    DefaultLoader.getIdeHelper().setProperties(CommonConst.CACHED_SPARK_SDK_PATHS, comboBoxAddSparkManually.getItems());
                }
            }
        });

        String[] tmp = DefaultLoader.getIdeHelper().getProperties(CommonConst.CACHED_SPARK_SDK_PATHS);
        if (tmp != null) {
            cachedLibraryPath.addAll(Arrays.asList(tmp));
        }

        for (int i = 0; i < cachedLibraryPath.size(); ++i) {
            comboBoxAddSparkManually.add(cachedLibraryPath.get(i));
            try {
                SparkLibraryInfoForEclipse info = new SparkLibraryInfoForEclipse(cachedLibraryPath.get(i));
                comboBoxAddSparkManually.setData(cachedLibraryPath.get(i), info);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                // do nothing if we can not get the library info
            }
        }

        if (cachedLibraryPath.size() > 0) {
            comboBoxAddSparkManually.select(0);
        }

        btnSelectSparkLib = new Button(comAddSparkManually, SWT.PUSH);
        btnSelectSparkLib.setText("Select...");
        btnSelectSparkLib.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                FileDialog dialog = new FileDialog(SparkLibraryOptionsPanel.this.getShell());
                String[] extensions = { "*.jar;*.JAR;*.zip;*.ZIP", };
                dialog.setFilterExtensions(extensions);
                String file = dialog.open();
                if (file != null) {
                    try {
                        comboBoxAddSparkManually.add(file, 0);
                        comboBoxAddSparkManually.setData(file, new SparkLibraryInfoForEclipse(file));
                        comboBoxAddSparkManually.select(0);
                        updateNextPageStatus();
                        DefaultLoader.getIdeHelper().setProperties(CommonConst.CACHED_SPARK_SDK_PATHS, comboBoxAddSparkManually.getItems());
                    } catch (Exception e) {
                        Activator.getDefault().log("Error adding Spark library", e);
                    }
                }
            }
        });

        tipLabel = new Link(this, SWT.FILL);
        tipLabel.setText(
                "You can either download Spark library from <a href=\"http://go.microsoft.com/fwlink/?LinkID=723585&clcid=0x409\">here</a> or add Apache Spark packages from Maven repository in the project manually.");
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        tipLabel.setLayoutData(gridData);
        tipLabel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    AppInsightsClient.create(Messages.HDInsightDownloadSparkLibrary, null);
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
                } catch (Exception ex) {
                    /*
                     * only logging the error in log file not showing anything
                     * to end user.
                     */
                    Activator.getDefault().log("Error opening link", ex);
                }
            }
        });

        enableAddSparkManually(false);
    }

    public boolean getUsingMaven() {
        return isUsingMaven;
    }

    public void setUsingMaven(boolean val) {
        isUsingMaven = val;
    }

    private void enableUseMaven(boolean toEnable) {
        isUsingMaven = toEnable;
        comUseMaven.setEnabled(toEnable);
        comboBoxUseMaven.setEnabled(toEnable);

        if (toEnable && MavenPluginUtil.checkMavenPluginInstallation() != true) {
            if (parentPage != null) {
                IWizard wizard = parentPage.getWizard();
                parentPage.getShell().close();
                if (wizard != null) {
                    wizard.performCancel();
                }
            }

            MavenPluginUtil.installMavenPlugin();
        }
    }

    private void enableAddSparkManually(boolean toEnable) {
        isUsingMaven = !toEnable;
        comAddSparkManually.setEnabled(toEnable);
        comboBoxAddSparkManually.setEnabled(toEnable);
        tipLabel.setEnabled(toEnable);
    }

    private void updateNextPageStatus() {
        if(parentPage.canFlipToNextPage()) {
            parentPage.setPageComplete(true);
        }
    }

    public String getSparkLibraryPath() {
        return comboBoxAddSparkManually.getText();
    }

    public SparkVersion getSparkVersion() {
        return sparkVersion;
    }
}
