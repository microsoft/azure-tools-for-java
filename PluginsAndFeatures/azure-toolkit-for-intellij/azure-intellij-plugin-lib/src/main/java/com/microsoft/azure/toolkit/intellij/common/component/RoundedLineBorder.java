/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import java.awt.*;

public class RoundedLineBorder extends com.intellij.ui.RoundedLineBorder {
    public RoundedLineBorder(Color color) {
        super(color);
    }

    public RoundedLineBorder(Color color, int thickness) {
        super(color, thickness);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        final Graphics2D g2 = (Graphics2D)g;
        final Object oldAntialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final Color oldColor = g2.getColor();
        g2.setColor(lineColor);
        for (int i = 0; i < thickness; i++) {
            g2.drawRoundRect(x + i, y + i, width - i - i - 1, height - i - i - 1, height, height);
        }
        g2.setColor(oldColor);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
    }
}
