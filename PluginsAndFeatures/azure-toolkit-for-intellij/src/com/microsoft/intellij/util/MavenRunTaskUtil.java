package com.microsoft.intellij.util;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.project.Project;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider;

import java.io.File;
import java.util.List;

public class MavenRunTaskUtil {

    private static final String MAVEN_TASK_PACKAGE = "package";

    public static boolean shouldAddMavenPackageTask(List<BeforeRunTask> tasks, Project project) {
        boolean shouldAdd = true;
        for (BeforeRunTask task: tasks) {
            if (task.getProviderId().equals(MavenBeforeRunTasksProvider.ID)) {
                MavenBeforeRunTask mavenTask = (MavenBeforeRunTask) task;
                if (mavenTask.getGoal().contains(MAVEN_TASK_PACKAGE) &&
                        mavenTask.getProjectPath().equals(project.getBasePath() + File.separator + MavenConstants.POM_XML)) {
                    mavenTask.setEnabled(true);
                    shouldAdd = false;
                    break;
                }
            }
        }
        return shouldAdd;
    }

    public static boolean isMavenProject(Project project) {
        return MavenProjectsManager.getInstance(project).isMavenizedProject();
    }

}
