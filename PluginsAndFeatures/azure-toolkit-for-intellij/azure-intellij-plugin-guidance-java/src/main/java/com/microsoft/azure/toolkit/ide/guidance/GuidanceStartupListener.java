package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.IIdeStore;
import com.microsoft.azure.toolkit.ide.guidance.config.CourseConfig;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class GuidanceStartupListener implements StartupActivity {
    private static final String GUIDANCE = "guidance";
    private static final String GUIDANCE_SHOWN = "guidance_shown";
    private static final String GUIDANCE_COURSES = "guidance_courses";

    @Override
    public void runActivity(@Nonnull Project project) {
        final CourseConfig courseConfigFromWorkspace = GuidanceConfigManager.getInstance().getCourseConfigFromWorkspace(project);
        if (Objects.nonNull(courseConfigFromWorkspace)) {
            GuidanceViewManager.getInstance().openCourseView(project, courseConfigFromWorkspace);
        } else {
            showGuidanceAtFirstStartup(project);
        }
    }

    private void showGuidanceAtFirstStartup(@Nonnull final Project project) {
        final IIdeStore ideStore = AzureStoreManager.getInstance().getIdeStore();
        if (Objects.isNull(ideStore)) {
            return;
        }
        final List<CourseConfig> courseConfigs = GuidanceConfigManager.getInstance().loadCourses();
        final String shownCourses = ideStore.getProperty(GUIDANCE, GUIDANCE_COURSES);
        final boolean isAllCoursesShownBefore = courseConfigs.stream().allMatch(courseConfig -> StringUtils.containsIgnoreCase(shownCourses, courseConfig.getName()));
        final boolean isGuidanceShownBefore = Optional.ofNullable(ideStore.getProperty(GUIDANCE, GUIDANCE_SHOWN)).map(Boolean::valueOf).orElse(false);
        if (!(isGuidanceShownBefore && isAllCoursesShownBefore)) {
            ideStore.setProperty(GUIDANCE, GUIDANCE_SHOWN, String.valueOf(true));
            ideStore.setProperty(GUIDANCE, GUIDANCE_COURSES, courseConfigs.stream().map(CourseConfig::getName).collect(Collectors.joining(",")));
            GuidanceViewManager.getInstance().showCoursesView(project);
        }
    }
}
