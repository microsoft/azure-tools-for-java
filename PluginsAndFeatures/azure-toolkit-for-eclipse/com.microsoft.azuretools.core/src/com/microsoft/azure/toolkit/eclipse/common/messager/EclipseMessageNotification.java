/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.messager;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class EclipseMessageNotification extends AbstractNotificationPopup implements EclipseMessageView {

    @Nonnull
    private final EclipseAzureMessage message;
    private Image icon;

    public EclipseMessageNotification(@Nonnull EclipseAzureMessage message, Display display) {
        super(display);
        this.message = message;
    }

    @Override
    public void createContentArea(Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        container.setLayout(new GridLayout(1, false));
        final Rectangle clientArea = parent.getShell().getDisplay().getClientArea();
        Browser browser = new Browser(container, SWT.NO_SCROLL);
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        browser.setText(this.getMessage().getContent());
        browser.setBackgroundMode(SWT.INHERIT_FORCE);
        int height = clientArea.height / 15;
        browser.setSize((int) (height * 3.5), height);
        Composite buttonBar = new Composite(container, SWT.NONE);
        buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false, 1, 1));
        buttonBar.setLayout(new GridLayout(2, false));
        Arrays.stream(this.getMessage().getActions()).forEach(a -> this.addActionButton(a, buttonBar));
    }

    @Override
    protected String getPopupShellTitle() {
        return Optional.ofNullable(this.message.getTitle()).filter(StringUtils::isNotEmpty).orElse("Azure Notification") ;
    }

    @Override
    protected Image getPopupShellImage() {
        int size = 18;
        final Image image = this.getIcon();
        final ImageData imgData = image.getImageData().scaledTo(size, size);
        this.icon = new Image(this.getDisplay(), imgData);
        return this.icon;
    }

    @Override
    public boolean close() {
        if (Objects.nonNull(this.icon)) {
            this.icon.dispose();
        }
        return super.close();
    }

    @Override
    public Display getDisplay() {
        Shell shell = super.getShell();
        if (shell == null || shell.isDisposed()) {
            shell = this.getParentShell();
        }
        return shell != null && !shell.isDisposed() ? shell.getDisplay() : Display.getCurrent();
    }

    @Override
    @Nonnull
    public EclipseAzureMessage getMessage() {
        return message;
    }
}
