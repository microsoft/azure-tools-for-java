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

package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ComboBox;
import com.microsoft.azure.hdinsight.common.logger.ILogger;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SbtVersionOptionsPanel extends JPanel implements ILogger {
    private ComboBox sbtVersionComboBox;

    public String apply() {
        return (String) this.sbtVersionComboBox.getSelectedItem();
    }

    public SbtVersionOptionsPanel() {
        sbtVersionComboBox = new ComboBox();
        add(sbtVersionComboBox);

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(sbtVersionComboBox, constraints);
        setLayout(layout);
    }

    public void updateSbtVersions() {
        final String[][] versions = new String[1][1];
        ProgressManager.getInstance().runProcess(() -> {
            versions[0] = getVersions();
        }, null);

        for (String version : versions[0]) {
            this.sbtVersionComboBox.addItem(version);
        }
    }

    public String[] getVersions() {
        // TODO:replace with the following code after IDEA 2019.2
        // Versions.SbtKind$.MODULE$.apply().versions();
        String[] defaultVersions = new String[]{"0.13.18", "1.2.8"};
        try {
            // use api for IDEA 2019.2
            Constructor sbtKindConstructor = Class.forName("org.jetbrains.plugins.scala.project.Versions$SbtKind$").getConstructor();
            Method apply = Class.forName("org.jetbrains.plugins.scala.project.Versions$Kind").getMethod("apply");
            Object module = apply.invoke(sbtKindConstructor.newInstance());
            Method versions = Class.forName("org.jetbrains.plugins.scala.project.Versions").getMethod("versions");
            return (String[]) versions.invoke(module);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // use old api for IDEA 2019.1
            try {
                log().warn("Encounter exception when getting sbt versions using 2019.2 api", ignored);
                Constructor versionsConstructor = Class.forName("org.jetbrains.plugins.scala.project.Versions$").getDeclaredConstructor();
                versionsConstructor.setAccessible(true);
                Object versions = versionsConstructor.newInstance();
                Method loadVersions = Class.forName("org.jetbrains.plugins.scala.project.Versions$").getMethod("loadSbtVersions");
                return (String[]) loadVersions.invoke(versions);
            } catch (Exception ignore) {
                log().warn("Encounter exception when getting sbt versions using 2019.1 api", ignored);
                return defaultVersions;
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
            log().warn("Encounter exception when getting sbt versions using 2019.2 api", ignored);
            return defaultVersions;
        }
    }
}
