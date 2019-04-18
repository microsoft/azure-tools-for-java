package com.microsoft.azuretools.ijidea.ui;

import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Created by vlashch on 2/13/17.
 */
public class HintTextField extends JTextField implements FocusListener {

    private final String hint;
    private boolean showingHint;

    public HintTextField(final String hint) {
        super(hint);
        setText(hint);
        this.hint = hint;
        setShowingHint(true);
        super.addFocusListener(this);
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (this.getText().isEmpty()) {
            setShowingHint(false);
            super.setText("");
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (this.getText().isEmpty()) {
            setShowingHint(true);
            super.setText(hint);
        }
    }

    @Override
    public String getText() {
        return showingHint ? "" : super.getText();
    }

    @Override
    public void setText(String t) {
        if (t == null || t.isEmpty()) return;
        super.setText(t);
        setShowingHint(false);
    }

    private void setShowingHint(boolean showingHint) {
        this.showingHint = showingHint;
        if (!showingHint) {
            if (UIUtil.isUnderDarcula()) {
                super.setForeground(new Color(187, 187, 187));
            } else {
                super.setForeground(Color.BLACK);
            }
        } else {
            super.setForeground(Color.GRAY);
        }
    }
}