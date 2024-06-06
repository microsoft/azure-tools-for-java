package com.microsoft.azure.toolkit.ide.guidance.task;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.util.SystemInfo;
import com.microsoft.azure.toolkit.ide.guidance.*;
import com.microsoft.azure.toolkit.ide.guidance.config.CourseConfig;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.transport.URIish;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class GitCloneTask implements Task {
    public static final String DIRECTORY = "directory";
    public static final String DEFAULT_GIT_DIRECTORY = "defaultGitDirectory";
    public static final String BRANCH = "branch";
    public static final String REPOSITORY = "repository";
    public static final String REPOSITORY_PATH = "repository_path";
    public static final String ORIGIN = "origin";
    private final Course course;
    private final ComponentContext context;

    public GitCloneTask(@Nonnull ComponentContext context) {
        this.context = context;
        this.course = context.getCourse();
        init();
    }


    @Override
    public boolean isDone() {
        // Check whether project was clone to local
        final CourseConfig workspaceConfig = GuidanceConfigManager.getInstance().getCourseConfigFromWorkspace(context.getProject());
        if (workspaceConfig != null && StringUtils.equals(workspaceConfig.getName(), course.getName())) {
            Optional.ofNullable(context.getProject().getBasePath()).ifPresent(path -> this.context.applyResult(DEFAULT_GIT_DIRECTORY, path));
            return true;
        } else {
            return false;
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.common.clone";
    }

    @Override
    @AzureOperation(name = "boundary/guidance.clone")
    public void execute() throws Exception {
        final String repository = (String) context.getParameter(REPOSITORY);
        final String branch = (String) context.getParameter(BRANCH);
        final String repositoryPath = (String) context.getParameter(REPOSITORY_PATH);
        final String directory = (String) context.getParameter(DIRECTORY);
        AzureMessager.getMessager().info(AzureString.format("Cloning project to %s...", directory));
        try {
            final File file = new File(directory);
            final Git git = Git.init().setDirectory(file).call();
            // add remote
            git.remoteAdd().setName(ORIGIN).setUri(new URIish(repository)).call();
            // set auto crlf to true in windows
            if (SystemInfo.isWindows) {
                git.getRepository().getConfig().setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, true);
            }
            // create new branch and check out
            git.fetch().setRemote(ORIGIN).call();
            git.branchCreate().setName(branch).setStartPoint(String.format("%s/%s", ORIGIN, branch)).call();
            git.checkout().setName(branch).call();
            AzureMessager.getMessager().info(AzureString.format("Clone project to %s successfully.", directory));
            // Copy get start file to path
            final File workspace = StringUtils.isEmpty(repositoryPath) ? file : new File(file, repositoryPath);
            copyConfigurationToWorkspace(workspace);
            ProjectUtil.openOrImport(workspace.toPath(), OpenProjectTask.build().asNewProject());
            if (!context.getProject().isDisposed()) {
                GuidanceViewManager.getInstance().closeGuidanceToolWindow(context.getProject());
            }
        } catch (final Exception t) {
            AzureMessager.getMessager().error(t);
            throw t instanceof AzureToolkitRuntimeException ? (AzureToolkitRuntimeException) t : new AzureToolkitRuntimeException(t);
        }
    }

    private void init() {
        final String userHome = System.getProperty("user.home");
        Path projectDir = Path.of(userHome, context.getCourse().getName());
        if (Files.isDirectory(projectDir)) {
            projectDir = Path.of(userHome, String.format("%s-%s", context.getCourse().getName(), Utils.getTimestamp()));
        }
        this.context.applyResult(DEFAULT_GIT_DIRECTORY, projectDir.toAbsolutePath().toString());
    }

    private void copyConfigurationToWorkspace(final File workspace) throws IOException {
        if (StringUtils.isEmpty(course.getUri())) {
            return;
        }
        final File configurationDirectory = GuidanceConfigManager.getInstance().initConfigurationDirectory(workspace.getAbsolutePath());
        try (final InputStream inputStream = GuidanceConfigManager.class.getResourceAsStream(course.getUri())) {
            if (inputStream == null) {
                return;
            }
            FileUtils.copyInputStreamToFile(inputStream, new File(configurationDirectory, GuidanceConfigManager.GETTING_START_CONFIGURATION_NAME));
        }
    }
}
