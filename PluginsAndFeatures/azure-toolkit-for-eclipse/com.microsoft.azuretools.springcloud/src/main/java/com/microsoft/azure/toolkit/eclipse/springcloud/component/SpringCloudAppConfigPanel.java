/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.springcloud.component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureTextInput;
import com.microsoft.azure.toolkit.eclipse.common.form.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;

public class SpringCloudAppConfigPanel extends Composite implements AzureFormPanel<SpringCloudAppConfig> {
	private Button useJava8;
	private Button useJava11;
	private Button useJava17;
	private AzureTextInput txtJvmOptions;
	private AzureTextInput envTable;
	private CCombo numCpu;
	private CCombo numMemory;
	private AzureSlider numInstance;

	private Consumer<? super SpringCloudAppConfig> listener = (config) -> {
	};
	private SpringCloudAppConfig originalConfig;
	private Button toggleEndpoint;
	private Button toggleStorage;
	private Label lblRuntime;
	private Label lblDisk;

	public SpringCloudAppConfigPanel(Composite parent) {
		super(parent, SWT.NONE);
		setupUI();
		this.init();
	}

	private void init() {
		final TailingDebouncer debouncer = new TailingDebouncer(this::onDataChanged, 300);
		this.toggleStorage.addListener(SWT.Selection, e -> {
			toggle(this.toggleStorage, this.toggleStorage.getSelection());
			debouncer.debounce();
		});
		this.toggleEndpoint.addListener(SWT.Selection, e -> {
			toggle(this.toggleEndpoint, this.toggleEndpoint.getSelection());
			debouncer.debounce();
		});

		this.useJava8.addListener(SWT.Selection, (e) -> debouncer.debounce());
		this.useJava11.addListener(SWT.Selection, (e) -> debouncer.debounce());
		this.useJava17.addListener(SWT.Selection, (e) -> debouncer.debounce());
		this.txtJvmOptions.addModifyListener(e -> debouncer.debounce());
		this.envTable.addModifyListener(e -> debouncer.debounce());
		this.numCpu.addModifyListener(e -> debouncer.debounce());
		this.numMemory.addModifyListener(e -> debouncer.debounce());
		this.numInstance.addValueChangedListener(e -> debouncer.debounce());
	}

	public void reset() {
		AzureTaskManager.getInstance()
				.runLater(() -> Optional.ofNullable(this.originalConfig).ifPresent(this::setValue));
	}

	public void setDataChangedListener(Consumer<? super SpringCloudAppConfig> listener) {
		this.listener = listener;
	}

	private void onDataChanged() {
		if (Objects.nonNull(this.originalConfig) && Objects.nonNull(this.listener)) {
			final SpringCloudAppConfig newConfig = this.getValue();
			this.listener.accept(newConfig);
		}
	}

	public synchronized void updateForm(@Nonnull SpringCloudApp app) {
		final String sku = app.getParent().getSku();
		final boolean enterprise = sku.toLowerCase().startsWith("e");
		this.lblDisk.setVisible(!enterprise);
		this.toggleStorage.setVisible(!enterprise);
		this.useJava8.setVisible(!enterprise);
		this.useJava11.setVisible(!enterprise);
		this.useJava17.setVisible(!enterprise);
		this.lblRuntime.setVisible(!enterprise);
		((GridData) this.lblDisk.getLayoutData()).exclude = enterprise;
		((GridData) this.toggleStorage.getLayoutData()).exclude = enterprise;
		((GridData) this.useJava8.getLayoutData()).exclude = enterprise;
		((GridData) this.useJava11.getLayoutData()).exclude = enterprise;
		((GridData) this.useJava17.getLayoutData()).exclude = enterprise;
		((GridData) this.lblRuntime.getLayoutData()).exclude = enterprise;
		final boolean basic = sku.toLowerCase().startsWith("b");
		final Double cpu = numCpu.getItemCount() < 1 || numCpu.getSelectionIndex() < 0 ? 1
				: Double.valueOf(numCpu.getItem(numCpu.getSelectionIndex()));
		final Double mem = numMemory.getItemCount() < 1 || numMemory.getSelectionIndex() < 0 ? 1
				: Double.valueOf(numMemory.getItem(numMemory.getSelectionIndex()));
		final String[] cpus = basic ? new String[] { "0.5", "1" } : new String[] { "0.5", "1", "2", "3", "4" };
		final String[] mems = basic ? new String[] { "0.5", "1", "2" }
				: new String[] { "0.5", "1", "2", "3", "4", "5", "6", "7", "8" };
		this.numCpu.setItems(cpus);
		this.numMemory.setItems(mems);
		this.numCpu.setText(
				Objects.isNull(cpu) ? "1" : (cpu > (basic ? 1 : 4)) ? "1" : cpu < 1 ? "0.5" : "" + cpu.intValue());
		this.numMemory.setText(
				Objects.isNull(mem) ? "1" : (mem > (basic ? 2 : 8)) ? "1" : mem < 1 ? "0.5" : "" + mem.intValue());
		this.numInstance.setMaximum(basic ? 25 : 500);
		this.numInstance.setMajorTickSpacing(basic ? 5 : 50);
		this.numInstance.setMinorTickSpacing(basic ? 1 : 10);
		this.numInstance.setMinimum(0);
		this.numInstance.redraw();
		this.layout(true, false);
	}

	public SpringCloudAppConfig getValue(@Nonnull SpringCloudAppConfig appConfig) { // get config from form
		final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
		final boolean isEnterpriseTier = this.useJava17.isVisible();
		if (isEnterpriseTier) {
			final String javaVersion = this.useJava17.getSelection() ? RuntimeVersion.JAVA_17.toString() :
				this.useJava11.getSelection() ? RuntimeVersion.JAVA_11.toString() : RuntimeVersion.JAVA_8.toString();
			deploymentConfig.setRuntimeVersion(javaVersion);
			deploymentConfig.setEnablePersistentStorage(this.toggleStorage.getSelection());
		} else {
			deploymentConfig.setRuntimeVersion(null);
			deploymentConfig.setEnablePersistentStorage(false);
		}
		appConfig.setIsPublic(this.toggleEndpoint.getSelection());
		deploymentConfig.setCpu(Double.valueOf(numCpu.getItem(numCpu.getSelectionIndex())));
		deploymentConfig.setMemoryInGB(Double.valueOf(numMemory.getItem(numMemory.getSelectionIndex())));
		deploymentConfig.setInstanceCount(numInstance.getValue());
		deploymentConfig.setJvmOptions(Optional.ofNullable(this.txtJvmOptions.getText()).map(String::trim).orElse(""));
		deploymentConfig.setEnvironment(getEnvironmentVariables());
		return appConfig;
	}

	private Map<String, String> getEnvironmentVariables() {
		final String[] strPairs = this.envTable.getText().split(";");
		return Arrays.stream(strPairs).map(strPair -> strPair.split("=")).filter(pair -> pair.length > 1)
				.collect(Collectors.toMap(pair -> pair[0], pair -> pair[1], (a, b) -> b, HashMap::new));
	}

	@Override
	public synchronized void setValue(SpringCloudAppConfig config) {
		this.originalConfig = config;
		final SpringCloudDeploymentConfig deployment = config.getDeployment();
		this.toggle(this.toggleEndpoint, config.getIsPublic());
		this.toggle(this.toggleStorage, deployment.getEnablePersistentStorage());
		
		String runtimeVersion = deployment.getRuntimeVersion();
		if (StringUtils.isNoneBlank(runtimeVersion)) {
			this.useJava17
					.setSelection(StringUtils.equalsIgnoreCase(runtimeVersion, RuntimeVersion.JAVA_17.toString()));
			this.useJava11
					.setSelection(StringUtils.equalsIgnoreCase(runtimeVersion, RuntimeVersion.JAVA_11.toString()));
			this.useJava8.setSelection(StringUtils.equalsIgnoreCase(runtimeVersion, RuntimeVersion.JAVA_8.toString()));
		}

		this.txtJvmOptions.setText(deployment.getJvmOptions());
		final Map<String, String> env = deployment.getEnvironment();
		final String strEnv = ObjectUtils.firstNonNull(env, Collections.emptyMap()).entrySet().stream()
				.map(e -> String.format("%s=%s", e.getKey(), e.getValue())).collect(Collectors.joining(";"));
		this.envTable.setText(strEnv);

		Optional.ofNullable(deployment.getCpu()).ifPresent(c -> this.numCpu.setText(c < 1 ? "0.5" : "1"));
		Optional.ofNullable(deployment.getMemoryInGB()).ifPresent(c -> this.numMemory.setText(c < 1 ? "0.5" : "1"));
		this.numInstance.setValue(Optional.ofNullable(deployment.getInstanceCount()).orElse(1));
	}

	@Nonnull
	@Override
	public SpringCloudAppConfig getValue() {
		final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder()
				.deployment(SpringCloudDeploymentConfig.builder().build()).build();
		this.getValue(appConfig);
		return appConfig;
	}

	public void setEnabled(boolean enable) {
		this.useJava8.setEnabled(enable);
		this.useJava11.setEnabled(enable);
		this.useJava17.setEnabled(enable);
		this.toggleEndpoint.setEnabled(enable);
		this.toggleStorage.setEnabled(enable);
		numCpu.setEnabled(enable);
		numMemory.setEnabled(enable);
		numInstance.setEnabled(enable);
		envTable.setEnabled(enable);
		txtJvmOptions.setEnabled(enable);
	}

	private void toggle(Button p, boolean enabled) {
		p.setText(enabled ? "Enabled" : "Disabled");
		p.setSelection(enabled);
	}

	private void setupUI() {
		setLayout(new GridLayout(1, false));

		Group grpConfiguration = new Group(this, SWT.NONE);
		grpConfiguration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpConfiguration.setText("Configuration");
		grpConfiguration.setLayout(new GridLayout(4, false));

		Label lblNewLabel = new Label(grpConfiguration, SWT.NONE);
		lblNewLabel.setText("Public endpoint:");

		this.toggleEndpoint = new Button(grpConfiguration, SWT.CHECK);
		toggleEndpoint.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		toggleEndpoint.setText("Disabled");

		this.lblDisk = new Label(grpConfiguration, SWT.NONE);
		this.lblDisk.setText("Persistent Storage:");

		this.toggleStorage = new Button(grpConfiguration, SWT.CHECK);
		toggleStorage.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		this.toggleStorage.setText("Disabled");

		this.lblRuntime = new Label(grpConfiguration, SWT.NONE);
		this.lblRuntime.setText("Runtime:");

		this.useJava8 = new Button(grpConfiguration, SWT.RADIO);
		this.useJava8.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		this.useJava8.setText("Java 8");

		this.useJava11 = new Button(grpConfiguration, SWT.RADIO);
		this.useJava11.setText("Java 11");
		this.useJava11.setSelection(true); // use java 11 by default

		this.useJava17 = new Button(grpConfiguration, SWT.RADIO);
		this.useJava17.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		this.useJava17.setText("Java 17");

		Label lblJvmOptions = new Label(grpConfiguration, SWT.NONE);
		lblJvmOptions.setText("JVM options:");
		this.txtJvmOptions = new AzureTextInput(grpConfiguration, SWT.BORDER);
		GridData gd_txtJvmOptions = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd_txtJvmOptions.minimumWidth = 300;
		this.txtJvmOptions.setLayoutData(gd_txtJvmOptions);
		this.txtJvmOptions.setLabeledBy(lblJvmOptions);

		Label lblEnvVariable = new Label(grpConfiguration, SWT.NONE);
		lblEnvVariable.setText("Env variables:");
		this.envTable = new AzureTextInput(grpConfiguration, SWT.BORDER);
		this.envTable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		this.envTable.setLabeledBy(lblEnvVariable);

		Group grpScalingUpout = new Group(this, SWT.NONE);
		grpScalingUpout.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpScalingUpout.setText("Scaling Up/Out");
		grpScalingUpout.setLayout(new GridLayout(2, false));

		Label lblVcpu = new Label(grpScalingUpout, SWT.NONE);
		GridData gd_lblVcpu = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblVcpu.widthHint = 100;
		lblVcpu.setLayoutData(gd_lblVcpu);
		lblVcpu.setText("vCPU:");

		this.numCpu = new CCombo(grpScalingUpout, SWT.BORDER | SWT.READ_ONLY);
		this.numCpu.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblNewLabel_1 = new Label(grpScalingUpout, SWT.NONE);
		lblNewLabel_1.setText("Memory/GB:");

		this.numMemory = new CCombo(grpScalingUpout, SWT.BORDER | SWT.READ_ONLY);
		this.numMemory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblNewLabel_2 = new Label(grpScalingUpout, SWT.NONE);
		lblNewLabel_2.setText("Instances:");

		this.numInstance = new AzureSlider(grpScalingUpout, SWT.NONE);
		this.numInstance.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		this.numInstance.setLabeledBy(lblNewLabel_2);
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
