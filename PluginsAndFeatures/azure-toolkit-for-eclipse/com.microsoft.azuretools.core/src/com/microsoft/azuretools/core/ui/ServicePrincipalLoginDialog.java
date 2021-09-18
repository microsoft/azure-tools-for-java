/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azuretools.core.ui;

import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azuretools.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ServicePrincipalLoginDialog extends Dialog {
    private static final String GUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"; //UUID v1-v5
    private static final Pattern GUID_PATTERN = Pattern.compile(GUID_REGEX, Pattern.CASE_INSENSITIVE);
    private Text txtTenantId;
    private Text txtClientId;
    private Text txtPassword;
    private Text txtCertificate;
    private Text txtJson;
    private Button radioPassword;
    private Button radioCertificate;
    protected static final int DEBOUNCE_DELAY = 500;

    @Override
    public void okPressed() {
        model = getData();
        super.okPressed();
    }

    /**
     * Create the dialog.
     *
     * @param parentShell
     */
    public ServicePrincipalLoginDialog(Shell parentShell) {
        super(parentShell);
        this.updateFromJsonEditor = new TailingDebouncer(() -> {
            AzureTaskManager.getInstance().runLater(() -> {
                json2UIComponents();
            });
        }, DEBOUNCE_DELAY);

        this.updateToJsonEditor = new TailingDebouncer(() -> {
            AzureTaskManager.getInstance().runLater(() -> {
                AuthConfiguration newData = getData();
                if (!equalsData(newData, model)) {
                    model = newData;
                    uiTextComponents2Json();
                }

            });
        }, DEBOUNCE_DELAY);


    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * Create contents of the dialog.
     *
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout gridLayout = (GridLayout) container.getLayout();
        gridLayout.marginRight = 10;
        gridLayout.marginLeft = 10;
        gridLayout.numColumns = 2;

        Label lblNewLabel = new Label(container, SWT.NONE);
        lblNewLabel.setText("Tenant Id: ");

        txtTenantId = new Text(container, SWT.BORDER);
        txtTenantId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblClientId = new Label(container, SWT.NONE);
        lblClientId.setText("Client Id: ");

        txtClientId = new Text(container, SWT.BORDER);
        txtClientId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblSecret = new Label(container, SWT.NONE);
        lblSecret.setText("Secret: ");

        Group composite = new Group(container, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        GridLayout glComposite = new GridLayout(2, false);
        glComposite.marginTop = 1;
        glComposite.marginRight = 10;
        glComposite.marginLeft = 10;
        glComposite.marginWidth = 0;
        composite.setLayout(glComposite);

        radioPassword = new Button(composite, SWT.RADIO);
        radioPassword.setSelection(true);
        radioPassword.setText("Password:");

        txtPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
        txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        radioCertificate = new Button(composite, SWT.RADIO);
        radioCertificate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        radioCertificate.setText("Certificate:");

        Composite compositeCert = new Composite(composite, SWT.NONE);
        compositeCert.setLayout(new FormLayout());
        GridData gdCompositeCert = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gdCompositeCert.widthHint = 191;
        compositeCert.setLayoutData(gdCompositeCert);

        txtCertificate = new Text(compositeCert, SWT.BORDER | SWT.READ_ONLY);
        FormData fdTxtCertificate = new FormData();
        fdTxtCertificate.top = new FormAttachment(0, 3);
        fdTxtCertificate.left = new FormAttachment(0, 0);
        txtCertificate.setLayoutData(fdTxtCertificate);

        Button btnOpenFileButton = new Button(compositeCert, SWT.CENTER);
        fdTxtCertificate.right = new FormAttachment(btnOpenFileButton, -6);
        FormData fdBtnOpenFileButton = new FormData();
        fdBtnOpenFileButton.top = new FormAttachment(0, 1);
        fdBtnOpenFileButton.right = new FormAttachment(100);
        btnOpenFileButton.setLayoutData(fdBtnOpenFileButton);
        btnOpenFileButton.setText("...");
        btnOpenFileButton.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(btnOpenFileButton.getShell(), SWT.OPEN);
                dlg.setText("Open");
                String path = dlg.open();
                if (path == null) return;
                txtCertificate.setText(path);
            }
        });
        btnOpenFileButton.setEnabled(false);

        Label lblJson = new Label(container, SWT.NONE);
        lblJson.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true, 1, 1));
        lblJson.setText("JSON:");

        Composite composite2 = new Composite(container, SWT.NONE);
        composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        composite2.setLayout(new FillLayout(SWT.HORIZONTAL));

        txtJson = new Text(composite2, SWT.MULTI | SWT.BORDER);
        txtJson.setLayoutData(new GridData(GridData.FILL_BOTH));
        txtJson.addModifyListener(event -> {
            this.updateFromJsonEditor.debounce();
        });

        radioCertificate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                btnOpenFileButton.setEnabled(true);
            }
        });

        radioPassword.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                btnOpenFileButton.setEnabled(false);
            }
        });

        Stream.of(txtCertificate
            , txtClientId, txtPassword, txtTenantId).forEach(d -> d.addModifyListener(r -> updateToJsonEditor.debounce()));
        Stream.of(radioCertificate,
            radioPassword).forEach(d -> d.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                updateToJsonEditor.debounce();
            }
        }));

        return container;
    }

    private void uiTextComponents2Json() {
        if (txtJson.isDisposed()) {
            return;
        }
        try {
            Map<String, String> map = new LinkedHashMap<>();
            if (model.getCertificate() != null) {
                map.put("fileWithCertAndPrivateKey", model.getCertificate());
            } else {
                String password = StringUtils.isNotBlank(model.getKey()) ? "<hidden>" : "<empty>";
                map.put("password", password);
            }
            map.put("appId", model.getClient());
            map.put("tenant", model.getTenant());
            String text = JsonUtils.getGson().toJson(map);

            if (!StringUtils.equals(
                txtJson.getText().replaceAll("\\s", ""), text.replaceAll("\\s", ""))) {
                txtJson.setText(text);
            }

        } finally {
            intermediateState.set(false);
        }
    }

    private static boolean equalsData(AuthConfiguration config1, AuthConfiguration config2) {
        if (config1 == config2) {
            return true;
        }
        if (config1 == null) {
            config1 = new AuthConfiguration();
        }
        if (config2 == null) {
            config2 = new AuthConfiguration();
        }
        return StringUtils.equals(config1.getClient(), config2.getClient()) &&
            StringUtils.equals(config1.getTenant(), config2.getTenant()) &&
            StringUtils.equals(config1.getCertificate(), config2.getCertificate()) &&
            StringUtils.equals(config1.getKey(), config2.getKey()) &&
            StringUtils.equals(config1.getCertificatePassword(), config2.getCertificatePassword());

    }

    private final Debouncer updateFromJsonEditor;
    private final Debouncer updateToJsonEditor;
    private AuthConfiguration model;

    public AuthConfiguration getModel() {
        return model;
    }

    public void setModel(AuthConfiguration model) {
        this.model = model;
    }

    public AuthConfiguration getData() {
        AuthConfiguration data = new AuthConfiguration();

        data.setClient(txtClientId.getText());
        data.setTenant(txtTenantId.getText());
        if (radioPassword.getSelection()) {
            data.setKey(String.valueOf(txtPassword.getText()));
        } else {
            data.setCertificate(this.txtCertificate.getText());
        }
        data.setType(AuthType.SERVICE_PRINCIPAL);
        return data;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(454, 379);
    }

    /**
     * Create contents of the button bar.
     *
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        button.setText("Sign In");
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    protected void configureShell(Shell shell) {
        shell.setMinimumSize(new Point(420, 339));
        super.configureShell(shell);
        shell.setText("Sign In - Service Principal");
    }

    private AtomicBoolean intermediateState = new AtomicBoolean(false);

    private void json2UIComponents() {
        if (txtJson.isDisposed()) {
            return;
        }

        try {
            String json = this.txtJson.getText();
            Map<String, String> map = JsonUtils.fromJson(json, HashMap.class);
            if (map == null) {
                return;
            }
            AuthConfiguration newData = new AuthConfiguration();
            if (map.containsKey("appId")) {
                newData.setClient(StringUtils.defaultString(map.get("appId")));
            }

            if (map.containsKey("tenant")) {
                newData.setTenant(StringUtils.defaultString(map.get("tenant")));
            }

            if (map.containsKey("password")) {
                newData.setKey(isPlaceHolder(map.get("password")) ? this.txtPassword.getText() : map.get("password"));
            }

            if (map.containsKey("fileWithCertAndPrivateKey")) {
                newData.setCertificate(StringUtils.defaultString(map.get("fileWithCertAndPrivateKey")));
            }
            setData(newData);
        } catch (JsonSyntaxException ex) {
            // ignore all json errors
        } finally {
            intermediateState.set(false);
        }
    }

    public void setData(AuthConfiguration newData) {
        if (equalsData(newData, model)) {
            return;
        }
        this.model = newData;
        this.txtTenantId.setText(StringUtils.defaultString(newData.getTenant()));
        this.txtClientId.setText(StringUtils.defaultString(newData.getClient()));

        if (!StringUtils.isAllBlank(model.getCertificate(), model.getKey())) {
            if (model.getKey() != null) {
                this.txtPassword.setText(model.getKey());
                this.radioPassword.setSelection(true);
                this.radioCertificate.setSelection(false);
            } else {
                this.txtCertificate.setText(model.getCertificate());
                this.radioPassword.setSelection(false);
                this.radioCertificate.setSelection(true);
            }
        }
    }

    private static boolean isPlaceHolder(String password) {
        return Arrays.asList("<hidden>", "<empty>").contains(password);
    }

    private static boolean isGuid(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        return GUID_PATTERN.matcher(str).matches();
    }
}
