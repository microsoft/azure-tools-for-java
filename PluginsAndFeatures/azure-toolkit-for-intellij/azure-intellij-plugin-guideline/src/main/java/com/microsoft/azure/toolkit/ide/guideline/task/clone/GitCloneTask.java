package com.microsoft.azure.toolkit.ide.guideline.task.clone;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.ProjectUtil;
import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.GuidanceConfigManager;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

public class GitCloneTask implements Task {
    private Process process;

    public GitCloneTask(Process process) {
        this.process = process;
    }

    @Override
    public InputComponent getInputComponent() {
        return new CloneTaskInputPanel(process);
    }

    @Override
    public void execute(Context context) {
        final String projectPath = (String) context.getProperty("directory");
        final String gitUrl = "https://github.com/spring-guides/gs-spring-boot.git";
        try {
            Git.cloneRepository()
                    .setURI(gitUrl)
                    .setDirectory(Paths.get(projectPath).toFile())
                    .call();
            // Copy get start file to path
            copyConfigurationToWorkspace(projectPath);
            ProjectUtil.openOrImport(Paths.get(projectPath, "complete"), OpenProjectTask.newProject());
        } catch (Exception ex) {
            AzureMessager.getMessager().error(ex);
            throw new AzureToolkitRuntimeException(ex);
        }
    }

    private void copyConfigurationToWorkspace(final String projectPath) throws IOException {
        try (final InputStream inputStream = GuidanceConfigManager.class.getResourceAsStream(process.getUri())) {
            final File complete = new File(projectPath, "complete");
            FileUtils.copyInputStreamToFile(inputStream, new File(complete, GuidanceConfigManager.GETTING_START_CONFIGURATION_NAME));
        } catch (final IOException e) {
            throw e;
        }
    }
}
