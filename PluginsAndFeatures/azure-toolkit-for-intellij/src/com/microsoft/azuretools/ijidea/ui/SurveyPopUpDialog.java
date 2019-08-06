package com.microsoft.azuretools.ijidea.ui;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.wm.impl.WindowManagerImpl;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.intellij.helpers.CustomerSurveyHelper;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SurveyPopUpDialog extends JDialog {

    private static final int DISPOSE_TIME = 10;
    private static final String LABEL_DO_NOT_SHOW_AGAIN = "Don't show again.";
    private static final String LABEL_PROMPT = "<html>" +
            "<h3 align=\"center\">Enjoy Azure Toolkits?</h3>" +
            "<p align=\"center\" style=\"line-height: 0%\"> Your feedback is important, </p>" +
            "<p align=\"center\" style=\"line-height: 0%\"> take a minute to fill out our survey.</p>" +
            "</html>";

    private JPanel contentPane;
    private JButton giveFeedbackButton;
    private JButton notNowButton;
    private HyperlinkLabel lblDoNotShowAgain;
    private JLabel lblMessage;
    private JLabel lblAzureIcon;
    private JLabel lblClose;

    private CustomerSurveyHelper customerSurveyHelper;

    private Point dragPosition;
    private Timer disposeTimer;
    private LafManagerListener themeListener;
    private Color buttonOnHoverColor = Color.WHITE;
    private boolean isDisposed = false;

    public SurveyPopUpDialog(CustomerSurveyHelper customerSurveyHelper, Project project) {
        super();

        this.customerSurveyHelper = customerSurveyHelper;
        this.disposeTimer = new Timer(1000 * DISPOSE_TIME, (e) -> this.putOff());
        this.themeListener = lafManager -> renderUiByTheme();

        this.setAlwaysOnTop(true);
        this.setSize(250, 250);
        this.setContentPane(contentPane);
        this.setUndecorated(true);
        this.getRootPane().setBorder(BorderFactory.createLineBorder(Color.GRAY));
        this.setModal(false);
        this.setLocationRelativeToIDE(project);
        this.setDisposeTimer();

        giveFeedbackButton.addActionListener((e) -> takeSurvey());
        giveFeedbackButton.setFocusable(false);

        notNowButton.addActionListener((e) -> putOff());
        notNowButton.setFocusable(false);

        lblClose.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                putOff();
            }
        });

        addDragListener();
        // call onCancel() when cross is clicked
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                putOff();
            }
        });
        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> putOff(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);

        // Add listener to intellij theme change
        LafManager.getInstance().addLafManagerListener(this.themeListener);
        renderUiByTheme();
        this.pack();
        this.disposeTimer.restart();
    }

    private void renderUiByTheme() {
        // Use default ui setting for mac
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        if (isMac) {
            return;
        }
        if (UIUtils.isUnderIntelliJTheme()) {
            UIUtils.setPanelBackGroundColor(contentPane, Color.WHITE);
            ButtonUI buttonUI = new MetalButtonUI();
            giveFeedbackButton.setUI(buttonUI);
            giveFeedbackButton.setForeground(new Color(255, 255, 255));
            giveFeedbackButton.setBackground(new Color(0, 114, 198));
            notNowButton.setUI(buttonUI);
            notNowButton.setForeground(new Color(255, 255, 255));
            notNowButton.setBackground(new Color(105, 105, 105));
            buttonOnHoverColor = Color.LIGHT_GRAY;
        } else {
            UIUtils.setPanelBackGroundColor(contentPane, null);
            ButtonUI buttonUI = new JButton().getUI();
            giveFeedbackButton.setForeground(null);
            giveFeedbackButton.setBackground(null);
            giveFeedbackButton.setUI(buttonUI);
            notNowButton.setForeground(null);
            notNowButton.setBackground(null);
            notNowButton.setUI(buttonUI);
            buttonOnHoverColor = Color.WHITE;
        }
        giveFeedbackButton.setBorderPainted(false);
        setButtonHoverListener(giveFeedbackButton);

        notNowButton.setBorderPainted(false);
        setButtonHoverListener(notNowButton);
    }

    private void setDisposeTimer() {
        this.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                SurveyPopUpDialog.this.disposeTimer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                SurveyPopUpDialog.this.disposeTimer.restart();
            }
        });
    }

    private void setButtonHoverListener(JButton button) {
        button.addMouseListener(new MouseInputAdapter() {
            Color originForegroundColor;

            @Override
            public void mouseEntered(MouseEvent e) {
                originForegroundColor = ((JButton) e.getSource()).getForeground();
                super.mouseEntered(e);
                button.setForeground(buttonOnHoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                button.setForeground(originForegroundColor);
            }
        });
    }

    // Set pop up window to right bottom side of IDE
    private void setLocationRelativeToIDE(Project project) {
        project = project != null ? project : ProjectManagerImpl.getInstance().getOpenProjects()[0];
        JFrame ideFrame = WindowManagerImpl.getInstance().getFrame(project);
        int locationX = ideFrame.getX() + ideFrame.getWidth() - this.getWidth();
        int locationY = ideFrame.getY() + ideFrame.getHeight() - this.getHeight();
        this.setLocation(locationX, locationY);
    }

    private void addDragListener() {
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragPosition = new Point(e.getX(), e.getY());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                SurveyPopUpDialog.this.setLocation(e.getX() + getX() - dragPosition.x, e.getY() + getY() - dragPosition.y);
            }
        });
    }

    private synchronized void takeSurvey() {
        if (!isDisposed) {
            customerSurveyHelper.takeSurvey();
            close();
        }
    }

    private synchronized void putOff() {
        if (!isDisposed) {
            customerSurveyHelper.putOff();
            close();
        }
    }

    private synchronized void neverShow() {
        if (!isDisposed) {
            customerSurveyHelper.neverShowAgain();
            close();
        }
    }

    private synchronized void close() {
        isDisposed = true;
        disposeTimer.stop();
        LafManager.getInstance().removeLafManagerListener(this.themeListener);
        dispose();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        lblDoNotShowAgain = new HyperlinkLabel(LABEL_DO_NOT_SHOW_AGAIN);
        lblDoNotShowAgain.addHyperlinkListener(e -> neverShow());

        lblMessage = new JLabel(LABEL_PROMPT);

        lblAzureIcon = new JLabel();
        lblAzureIcon.setIcon(PluginUtil.getIcon("/icons/azure_large.png", 50, 50));
    }
}
