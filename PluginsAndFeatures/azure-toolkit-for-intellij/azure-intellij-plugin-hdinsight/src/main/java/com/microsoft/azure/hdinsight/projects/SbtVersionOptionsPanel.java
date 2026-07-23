/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.ComboBox;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import scala.collection.immutable.Seq;
import scala.reflect.ClassTag;

import javax.swing.*;
import java.awt.*;
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
            try {
                // In newer Scala plugin versions, the API changed. Use reflection for cross-version compatibility.
                // Old: Versions.SBT$.MODULE$.loadVersionsWithProgress(null).versions() -> Seq<String>
                // New: Versions.loadSbtVersions(false, null) -> Seq<SbtVersion>
                final Class<?> versionsClass = Class.forName("org.jetbrains.plugins.scala.project.Versions");
                try {
                    // Try new API first
                    final Method loadSbtVersionsMethod = versionsClass.getMethod("loadSbtVersions", boolean.class, com.intellij.openapi.progress.ProgressIndicator.class);
                    final Seq<?> sbtVersions = (Seq<?>) loadSbtVersionsMethod.invoke(null, false, null);
                    final int size = sbtVersions.size();
                    final String[] result = new String[size];
                    for (int i = 0; i < size; i++) {
                        result[i] = sbtVersions.apply(i).toString();
                    }
                    versions[0] = result;
                } catch (final NoSuchMethodException e) {
                    // Fallback to old API
                    final Class<?> versionsSbtClass = Class.forName("org.jetbrains.plugins.scala.project.Versions$SBT$");
                    final Object module = versionsSbtClass.getField("MODULE$").get(null);
                    final Method loadMethod = module.getClass().getMethod("loadVersionsWithProgress", com.intellij.openapi.progress.ProgressIndicator.class);
                    final Object loadedVersions = loadMethod.invoke(module, (Object) null);
                    final Method versionsMethod = loadedVersions.getClass().getMethod("versions");
                    final Seq<String> versionSeq = (Seq<String>) versionsMethod.invoke(loadedVersions);
                    versions[0] = (String[]) versionSeq.toArray(ClassTag.apply(String.class));
                }
            } catch (final Exception e) {
                log().warn("Failed to get SBT versions from scala plugin.", e);
                versions[0] = new String[0];
            }
        }, null);

        for (String version : versions[0]) {
            this.sbtVersionComboBox.addItem(version);
        }
    }
}
