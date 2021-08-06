/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers;

import com.microsoft.intellij.AzurePlugin;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.net.URI;
import java.util.Map;


public class LinkListener implements MouseListener {
    private Font original;
    private String mLink;

    public LinkListener(String link) {
        mLink = link;
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        try {
            Desktop.getDesktop().browse(new URI(mLink));
        } catch (Exception e) {
            AzurePlugin.log(e.getStackTrace().toString());
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    public void mouseEntered(MouseEvent mouseEvent) {
        mouseEvent.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));

        original = mouseEvent.getComponent().getFont();
        Map attributes = original.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        mouseEvent.getComponent().setFont(original.deriveFont(attributes));
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        mouseEvent.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        mouseEvent.getComponent().setFont(original);
    }
}
