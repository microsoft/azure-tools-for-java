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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

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
	private AzureTextInput txtJvmOptions;
	private AzureTextInput envTable;
	private Spinner numCpu;
	private Spinner numMemory;
	private AzureSlider numInstance;

	private Consumer<? super SpringCloudAppConfig> listener = (config) -> {
	};
	private SpringCloudAppConfig originalConfig;
	private Button toggleEndpoint;
	private Button toggleStorage;

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
		final boolean basic = sku.toLowerCase().startsWith("b");
		final int cpu = this.numCpu.getSelection();
		final int mem = this.numMemory.getSelection();
		final int maxCpu = basic ? 1 : 4;
		final int maxMem = basic ? 2 : 8;
		this.numCpu.setSelection(Math.min(cpu, maxCpu));
		this.numCpu.setMinimum(1);
		this.numCpu.setMaximum(maxCpu);
		this.numMemory.setSelection(Math.min(mem, maxMem));
		this.numMemory.setMinimum(1);
		this.numMemory.setMaximum(maxMem);
		this.numInstance.setMaximum(basic ? 25 : 500);
		this.numInstance.setMajorTickSpacing(basic ? 5 : 50);
		this.numInstance.setMinorTickSpacing(basic ? 1 : 10);
		this.numInstance.setMinimum(0);
		this.numInstance.redraw();
	}

	public SpringCloudAppConfig getValue(@Nonnull SpringCloudAppConfig appConfig) { // get config from form
		final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
		final String javaVersion = this.useJava11.getSelection() ? RuntimeVersion.JAVA_11.toString()
				: RuntimeVersion.JAVA_8.toString();
		appConfig.setIsPublic(this.toggleEndpoint.getSelection());
		deploymentConfig.setRuntimeVersion(javaVersion);
		deploymentConfig.setEnablePersistentStorage(this.toggleStorage.getSelection());
        deploymentConfig.setCpu(numCpu.getSelection() * 1.0);
        deploymentConfig.setMemoryInGB(numMemory.getSelection() * 1.0);
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
		final boolean java11 = StringUtils.equalsIgnoreCase(deployment.getRuntimeVersion(),
		        RuntimeVersion.JAVA_11.toString());
		this.useJava11.setSelection(java11);
		this.useJava8.setSelection(!java11);

		this.txtJvmOptions.setText(deployment.getJvmOptions());
		final Map<String, String> env = deployment.getEnvironment();
		final String strEnv = ObjectUtils.firstNonNull(env, Collections.emptyMap()).entrySet().stream()
				.map(e -> String.format("%s=%s", e.getKey(), e.getValue())).collect(Collectors.joining(";"));
		this.envTable.setText(strEnv);

        this.numCpu.setSelection(Optional.ofNullable(deployment.getCpu()).map(d -> d.intValue()).orElse(1));
        this.numMemory.setSelection(Optional.ofNullable(deployment.getMemoryInGB()).map(d -> d.intValue()).orElse(1));
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
		grpConfiguration.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(grpConfiguration, SWT.NONE);
		lblNewLabel.setText("Public endpoint:");

		this.toggleEndpoint = new Button(grpConfiguration, SWT.CHECK);
		toggleEndpoint.setText("Disabled");
		new Label(grpConfiguration, SWT.NONE);

		Label lblPersistentStorage = new Label(grpConfiguration, SWT.NONE);
		GridData gd_lblPersistentStorage = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblPersistentStorage.widthHint = 100;
		lblPersistentStorage.setLayoutData(gd_lblPersistentStorage);
		lblPersistentStorage.setText("Persistent storage:");

		this.toggleStorage = new Button(grpConfiguration, SWT.CHECK);
		this.toggleStorage.setText("Disabled");
		new Label(grpConfiguration, SWT.NONE);

		Label lblRuntime = new Label(grpConfiguration, SWT.NONE);
		lblRuntime.setText("Runtime:");

		this.useJava8 = new Button(grpConfiguration, SWT.RADIO);
		this.useJava8.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		this.useJava8.setText("Java 8");
		this.useJava8.setSelection(true);

		this.useJava11 = new Button(grpConfiguration, SWT.RADIO);
		this.useJava11.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		this.useJava11.setText("Java 11");

		Label lblJvmOptions = new Label(grpConfiguration, SWT.NONE);
		lblJvmOptions.setText("JVM options:");

		this.txtJvmOptions = new AzureTextInput(grpConfiguration, SWT.BORDER);
		this.txtJvmOptions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		this.txtJvmOptions.setLabeledBy(lblJvmOptions);

		Label lblEnvVariable = new Label(grpConfiguration, SWT.NONE);
		lblEnvVariable.setText("Env variables:");

		this.envTable = new AzureTextInput(grpConfiguration, SWT.BORDER);
		this.envTable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
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

		this.numCpu = new Spinner(grpScalingUpout, SWT.BORDER);
		numCpu.setSelection(1);
		this.numCpu.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblNewLabel_1 = new Label(grpScalingUpout, SWT.NONE);
		lblNewLabel_1.setText("Memory/GB:");

		this.numMemory = new Spinner(grpScalingUpout, SWT.BORDER);
		numMemory.setSelection(1);
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
