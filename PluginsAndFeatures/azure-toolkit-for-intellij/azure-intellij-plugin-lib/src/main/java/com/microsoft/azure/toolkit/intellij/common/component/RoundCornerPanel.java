package com.microsoft.azure.toolkit.intellij.common.component;

import javax.swing.*;
import java.awt.*;

public class RoundCornerPanel extends JPanel {
    private Color backgroundColor;

    private int cornerRadius = 8;

    public RoundCornerPanel() {
        super();
    }

    public RoundCornerPanel(LayoutManager layout, int radius) {
        super(layout);
        cornerRadius = radius;
    }

    public RoundCornerPanel(LayoutManager layout, int radius, Color bgColor) {
        super(layout);
        cornerRadius = radius;
        backgroundColor = bgColor;
    }

    public RoundCornerPanel(int radius) {
        super();
        cornerRadius = radius;
    }

    public RoundCornerPanel(int radius, Color bgColor) {
        super();
        cornerRadius = radius;
        backgroundColor = bgColor;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Dimension arcs = new Dimension(cornerRadius, cornerRadius);
        final int width = getWidth();
        final int height = getHeight();
        final Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //Draws the rounded panel with borders.
        if (backgroundColor != null) {
            graphics.setColor(backgroundColor);
        } else {
            graphics.setColor(getBackground());
        }
        graphics.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height); //paint background
        graphics.setColor(getForeground());
    }
}
