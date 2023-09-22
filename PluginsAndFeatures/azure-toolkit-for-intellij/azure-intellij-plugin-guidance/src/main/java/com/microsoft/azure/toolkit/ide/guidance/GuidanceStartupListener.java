package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.ide.guidance.config.CourseConfig;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuidanceStartupListener implements ProjectActivity {
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        final CourseConfig courseConfigFromWorkspace = GuidanceConfigManager.getInstance().getCourseConfigFromWorkspace(project);
        if (Objects.nonNull(courseConfigFromWorkspace)) {
            GuidanceViewManager.getInstance().openCourseView(project, courseConfigFromWorkspace);
        } else {
            showGuidanceAtFirstStartup(project);
        }
        return null;
    }

    private void showGuidanceAtFirstStartup(@Nonnull final Project project) {
        final AzureConfiguration config = Azure.az().config();
        final List<CourseConfig> courseConfigs = GuidanceConfigManager.getInstance().loadCourses();
        final String shownCourses = config.get(GuidanceConfigManager.GUIDANCE_COURSES);
        final boolean isAllCoursesShownBefore = courseConfigs.stream().allMatch(courseConfig -> StringUtils.containsIgnoreCase(shownCourses, courseConfig.getName()));
        final boolean isGuidanceShownBefore = config.get(GuidanceConfigManager.GUIDANCE_SHOWN, false);
        if (!(isGuidanceShownBefore && isAllCoursesShownBefore)) {
            config.set(GuidanceConfigManager.GUIDANCE_SHOWN, true);
            config.set(GuidanceConfigManager.GUIDANCE_COURSES, courseConfigs.stream().map(CourseConfig::getName).collect(Collectors.joining(",")));
            GuidanceViewManager.getInstance().showCoursesView(project);
        }
    }
}
