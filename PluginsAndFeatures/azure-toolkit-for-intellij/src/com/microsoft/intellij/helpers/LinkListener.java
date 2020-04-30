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
