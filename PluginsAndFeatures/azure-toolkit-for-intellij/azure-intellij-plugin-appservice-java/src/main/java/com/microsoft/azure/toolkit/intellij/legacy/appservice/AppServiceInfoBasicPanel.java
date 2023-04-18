/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.platform.RuntimeComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.SneakyThrows;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.microsoft.azuretools.utils.WebAppUtils.isSupportedArtifactType;

public class AppServiceInfoBasicPanel<T extends AppServiceConfig> extends JPanel implements AzureFormPanel<T> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");
    private static final int RG_NAME_MAX_LENGTH = 90;
    private static final int SP_NAME_MAX_LENGTH = 40;
    private final Project project;
    private final Supplier<? extends T> supplier;
    private T config;

    private JPanel contentPanel;

    private AppNameInput textName;
    private RuntimeComboBox selectorRuntime;
    private AzureArtifactComboBox selectorApplication;
    private TitledSeparator deploymentTitle;
    private JLabel lblArtifact;
    private JLabel lblName;
    private JLabel lblPlatform;

    public AppServiceInfoBasicPanel(final Project project, @Nonnull final Subscription subscription, final Supplier<? extends T> defaultConfigSupplier) {
        super();
        this.project = project;
        this.supplier = defaultConfigSupplier;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init(subscription);
    }

    private void init(Subscription subscription) {
        this.textName.setRequired(true);
        this.textName.setSubscription(subscription);
        this.selectorRuntime.setRequired(true);

        this.selectorApplication.setFileFilter(virtualFile -> {
            final String ext = FileNameUtils.getExtension(virtualFile.getPath());
            final Runtime platform = this.selectorRuntime.getValue();
            return StringUtils.isNotBlank(ext) && isSupportedArtifactType(platform, ext);
        });
        this.setDeploymentVisible(false);
        this.config = supplier.get();
        setValue(this.config);

        this.lblName.setLabelFor(textName);
        this.lblPlatform.setLabelFor(selectorRuntime);
        this.lblArtifact.setLabelFor(selectorApplication);
    }

    @SneakyThrows
    @Override
    public T getValue() {
        final String name = this.textName.getValue();
        final Runtime platform = this.selectorRuntime.getValue();
        final AzureArtifact artifact = this.selectorApplication.getValue();

        final T result = this.config == null ? supplier.get() : this.config;
        result.setName(name);
        result.setRuntime(platform);

        if (Objects.nonNull(artifact)) {
            final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.project);
            final String path = this.selectorApplication.getValue().getFileForDeployment();
            result.setApplication(Paths.get(path));
        }
        this.config = result;
        return result;
    }

    @Override
    public void setValue(final T config) {
        this.config = config;
        this.textName.setValue(config.getName());
        this.textName.setSubscription(config.getSubscription());
        this.selectorRuntime.setValue(config.getRuntime());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.textName,
            this.selectorRuntime
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    public RuntimeComboBox getSelectorRuntime() {
        return selectorRuntime;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.selectorApplication = new AzureArtifactComboBox(project, true);
    }

    public void setDeploymentVisible(boolean visible) {
        this.deploymentTitle.setVisible(visible);
        this.lblArtifact.setVisible(visible);
        this.selectorApplication.setVisible(visible);
    }

    public void setFixedRuntime(final Runtime runtime) {
        selectorRuntime.setPlatformList(Collections.singletonList(runtime));
        lblPlatform.setVisible(false);
        selectorRuntime.setVisible(false);
    }
}
