package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.ide.actions.runAnything.RunAnythingManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class MavenProjectListener implements ToolWindowManagerListener {

    private final Project project;

    MavenProjectListener(Project project) {
        this.project = project;
    }

    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {

        boolean mavenProject = MavenRunTaskUtil.isMavenProject(project);
        if (mavenProject) {
            try {
                // Replace "mvn clean install" with your desired Maven command
                File projectDirectory = new File(this.project.getBaseDir().getPath());
                System.out.println("The project directory is " + projectDirectory.getAbsolutePath());
                if (projectDirectory.exists()) {
                    System.out.println("Executing mvn clean package command");

                    Process process1 = new ProcessBuilder("java",  "-version")
                            .directory(projectDirectory)
                            .inheritIO()
                            .start();
                    process1.waitFor();
                    System.out.println("Completed running dir command");

                    Process process = new ProcessBuilder("mvn.cmd", "clean", "package", "azure:run")
                            .directory(projectDirectory)
                            .inheritIO()
                            .start();
                    System.out.println("Started mvn command execution");

                    process.waitFor(); // Wait for the process to complete
                    System.out.println("Completed mvn command execution");
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("Not a maven project");
        }
        VirtualFile projectFile = project.getProjectFile();
        VirtualFile workspaceFile = project.getWorkspaceFile();
        System.out.println("SPN: State changed in maven project listener");
    }
}
