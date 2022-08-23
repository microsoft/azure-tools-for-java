/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.JsonSyntaxException;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.spellchecker.ui.SpellCheckingEditorCustomization;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.ui.SoftWrapsEditorCustomization;
import com.microsoft.azure.toolkit.intellij.common.AzureCommentLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ServicePrincipalLoginDialog extends AzureDialog<AuthConfiguration> implements AzureForm<AuthConfiguration> {
    private static final String GUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"; //UUID v1-v5
    private static final Pattern GUID_PATTERN = Pattern.compile(GUID_REGEX, Pattern.CASE_INSENSITIVE);
    private JTextField clientIdTextField;
    private JTextField tenantIdTextField;
    private JPanel rootPanel;
    private EditorTextField jsonDataEditor;
    private JRadioButton passwordRadioButton;
    private JPasswordField keyPasswordField;
    private JRadioButton certificateRadioButton;
    private TextFieldWithBrowseButton certFileTextField;
    private AzureCommentLabel comment;
    private final Project project;
    private final AtomicBoolean intermediateState = new AtomicBoolean(false);

    public ServicePrincipalLoginDialog(@Nonnull Project project) {
        super(project);
        this.project = project;

        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.

        init();
        super.setOKButtonText("Sign In");

        initJsonData();

        // initialize cert file select
        final FileChooserDescriptor pem = FileChooserDescriptorFactory.createSingleFileDescriptor("pem");
        pem.withFileFilter(file -> StringUtils.equalsIgnoreCase(file.getExtension(), "pem"));
        certFileTextField.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>("Select Certificate File", null, certFileTextField, null,
            pem, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
            @Nullable
            protected VirtualFile getInitialFile() {
                return LocalFileSystem.getInstance().findFileByPath(FileUtil.getDirectoryWithinUserHome("/").toString());
            }
        });

        Stream.of(clientIdTextField, tenantIdTextField, keyPasswordField, certFileTextField.getTextField()).map(JTextComponent::getDocument)
            .forEach(document -> document.addDocumentListener(new TextDocumentListenerAdapter() {
                @Override
                public void onDocumentChanged() {
                    uiTextComponents2Json();
                }
            }));

        this.certificateRadioButton.addActionListener(e -> uiTextComponents2Json());
        this.passwordRadioButton.addActionListener(e -> uiTextComponents2Json());

        this.jsonDataEditor.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                json2UIComponents(jsonDataEditor.getText());
            }
        });
        certificateRadioButton.addActionListener(e -> syncComponentStatusWhenRadioButtonChanges());
        passwordRadioButton.addActionListener(e -> syncComponentStatusWhenRadioButtonChanges());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return rootPanel;
    }

    @Override
    protected @NotNull List<ValidationInfo> doValidateAll() {
        final List<ValidationInfo> res = new ArrayList<>();
        final AuthConfiguration data = getValue();
        if (StringUtils.isBlank(data.getTenant())) {
            res.add(new ValidationInfo("Tenant is required.", tenantIdTextField));
        }
        if (notGuid(data.getTenant())) {
            res.add(new ValidationInfo("Tenant should be a valid guid.", tenantIdTextField));
        }

        if (StringUtils.isBlank(data.getClient())) {
            res.add(new ValidationInfo("ClientId(appId) is required.", clientIdTextField));
        }
        if (notGuid(data.getClient())) {
            res.add(new ValidationInfo("ClientId(appId) should be a valid guid.", clientIdTextField));
        }

        if (this.passwordRadioButton.isSelected()) {
            if (StringUtils.isBlank(data.getKey())) {
                res.add(new ValidationInfo("Password is required.", keyPasswordField));
            }
        } else {
            if (StringUtils.isBlank(data.getCertificate())) {
                res.add(new ValidationInfo("Please select a cert file.", certFileTextField));
            } else if (!new File(data.getCertificate()).exists()) {
                res.add(new ValidationInfo(String.format("Cannot find cert file(%s).", certFileTextField.getText()), certFileTextField));
            }
        }
        return res;
    }

    @Override
    public AzureForm<AuthConfiguration> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Sign In - Service Principal";
    }

    private void syncComponentStatusWhenRadioButtonChanges() {
        certFileTextField.setEnabled(certificateRadioButton.isSelected());
        keyPasswordField.setEnabled(passwordRadioButton.isSelected());
    }

    private void createUIComponents() {
        this.jsonDataEditor = this.buildCodeViewer();
        this.comment = new AzureCommentLabel("You can copy the JSON output of 'az ad sp create-for-rbac ...' and paste it here");
    }

    private EditorTextField buildCodeViewer() {
        final EditorTextFieldProvider service = ApplicationManager.getApplication().getService(EditorTextFieldProvider.class);
        final Set<EditorCustomization> editorFeatures = new HashSet<>();
        editorFeatures.add(SoftWrapsEditorCustomization.ENABLED);
        editorFeatures.add(SpellCheckingEditorCustomization.getInstance(false));
        final EditorTextField editorTextField = service.getEditorField(Objects.requireNonNull(Language.findLanguageByID("JSON")), project, editorFeatures);
        editorTextField.setMinimumSize(new Dimension(200, 100));
        return editorTextField;
    }

    public AuthConfiguration getValue() {
        final AuthConfiguration data = new AuthConfiguration(AuthType.SERVICE_PRINCIPAL);

        data.setClient(clientIdTextField.getText());
        data.setTenant(tenantIdTextField.getText());
        if (passwordRadioButton.isSelected()) {
            data.setKey(String.valueOf(keyPasswordField.getPassword()));
        } else {
            data.setCertificate(this.certFileTextField.getText());
        }
        data.setType(AuthType.SERVICE_PRINCIPAL);
        return data;
    }

    @Override
    public void setValue(AuthConfiguration data) {
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return new ArrayList<>();
    }

    @Override
    public List<AzureValidationInfo> validateData() {
        return Collections.emptyList();
    }

    private void initJsonData() {
        final String textFromClip = findTextInClipboard(str ->
            StringUtils.contains(str, "appId") && StringUtils.contains(str, "tenant") &&
                (StringUtils.contains(str, "password") || StringUtils.contains(str, "fileWithCertAndPrivateKey"))
        );
        if (StringUtils.isNotBlank(textFromClip)) {
            json2UIComponents(textFromClip);
        }
        uiTextComponents2Json();
    }

    @javax.annotation.Nullable
    public static String findTextInClipboard(Predicate<String> func) {
        if (func == null) {
            return null;
        }
        for (final Transferable currentItem : CopyPasteManager.getInstance().getAllContents()) {
            if (currentItem.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    final String itemStr = currentItem.getTransferData(DataFlavor.stringFlavor).toString();
                    if (func.test(itemStr)) {
                        return itemStr;
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            // only for the first clip board
            break;
        }

        return null;
    }

    private void uiTextComponents2Json() {
        if (!intermediateState.compareAndSet(false, true)) {
            return;
        }
        try {
            final Map<String, String> map = new LinkedHashMap<>();
            final AuthConfiguration data = getValue();

            if (this.certificateRadioButton.isSelected()) {
                map.put("fileWithCertAndPrivateKey", data.getCertificate());
            } else {
                final String password = StringUtils.isNotBlank(data.getKey()) ? "<hidden>" : "<empty>";
                map.put("password", password);
            }
            map.put("appId", data.getClient());
            map.put("tenant", data.getTenant());
            final String text = JsonUtils.toJson(map);
            if (!StringUtils.equals(jsonDataEditor.getText(), text)) {
                this.jsonDataEditor.setText(text);
                this.jsonDataEditor.setCaretPosition(0);
            }
        } finally {
            intermediateState.set(false);
        }
    }

    private void json2UIComponents(String json) {
        try {
            final TypeReference<HashMap<String, String>> type = new TypeReference<>() {};
            final Map<String, String> map = JsonUtils.fromJson(json, type);
            if (map != null) {
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    if (!intermediateState.compareAndSet(false, true)) {
                        return;
                    }

                    try {
                        if (map.containsKey("appId")) {
                            this.clientIdTextField.setText(StringUtils.defaultString(map.get("appId")));
                        }

                        if (map.containsKey("tenant")) {
                            this.tenantIdTextField.setText(StringUtils.defaultString(map.get("tenant")));
                        }

                        if (map.containsKey("password") && !isPlaceHolder(map.get("password"))) {
                            this.passwordRadioButton.setSelected(true);
                            this.keyPasswordField.setText(StringUtils.defaultString(map.get("password")));
                        }

                        if (map.containsKey("fileWithCertAndPrivateKey")) {
                            this.certificateRadioButton.setSelected(true);
                            this.certFileTextField.setText(StringUtils.defaultString(map.get("fileWithCertAndPrivateKey")));
                        }
                    } finally {
                        intermediateState.set(false);
                    }

                });
            }

        } catch (final JsonSyntaxException ex) {
            // ignore all json errors
        }
    }

    private static boolean isPlaceHolder(String password) {
        return Arrays.asList("<hidden>", "<empty>").contains(password);
    }

    private static boolean notGuid(String str) {
        if (StringUtils.isBlank(str)) {
            return true;
        }
        return !GUID_PATTERN.matcher(str).matches();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
