package com.microsoft.azure.hdinsight.projects;

import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class ProjectUtil {
    private static final String downloadSparkSDKUrl = "http://go.microsoft.com/fwlink/?LinkID=723585&clcid=0x409";

    public static JPanel createSparkSDKTipsPanel() {
        final JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();

        JLabel[] labels = new JLabel[]{
                new JLabel("You can either download Spark library from"),
                new JLabel("<HTML><FONT color=\"#000099\"><U>here</U></FONT>,</HTML>"),
                new JLabel("or add Apache Spark packages from Maven repository in the project manually.")
        };

        for (int i = 0; i < labels.length; ++i) {
            panel.add(labels[i]);
        }

        labels[1].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        labels[1].setToolTipText(downloadSparkSDKUrl);
        labels[1].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    StringSelection stringSelection = new StringSelection(downloadSparkSDKUrl);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, null);
                    JOptionPane.showMessageDialog(panel,"Already copy Download URL to Clipboard");
                } else if(SwingUtilities.isLeftMouseButton(e)){
                    try
                    {
                        URI uri = new URI(downloadSparkSDKUrl);
                        Desktop.getDesktop().browse(uri);
                    }catch (Exception exception) {
                        DefaultLoader.getUIHelper().showError(exception.getMessage(), exception.getClass().getName());
                    }
                }
            }
        });

        GridBagConstraints constraints = new GridBagConstraints(GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE,
                1, 1,
                1, 1,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0);

        layout.setConstraints(labels[0], constraints);
        layout.setConstraints(labels[1], constraints);
        layout.setConstraints(labels[2], constraints);

        JPanel mainPanel = new JPanel();
        GridBagLayout mainLayout = new GridBagLayout();
        mainPanel.setLayout(mainLayout);

        mainPanel.add(panel, new GridBagConstraints(0,0,
                1,1,
                0,0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));

        //make sure label message on the head of left
        mainPanel.add(new JLabel(), new GridBagConstraints(1,0,
                1,1,
                1,1,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));

        return mainPanel;
    }
}
