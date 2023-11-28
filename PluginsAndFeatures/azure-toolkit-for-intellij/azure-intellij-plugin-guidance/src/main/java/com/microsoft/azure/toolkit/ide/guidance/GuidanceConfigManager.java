package com.microsoft.azure.toolkit.ide.guidance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.microsoft.azure.toolkit.ide.guidance.config.CourseConfig;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuidanceConfigManager {
    public static final String AZURE = ".azure";
    public static final String GETTING_START_CONFIGURATION_NAME = "azure-getting-started.json";

    private static final GuidanceConfigManager instance = new GuidanceConfigManager();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    private static final ObjectMapper JSON_MAPPER = new JsonMapper()
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static GuidanceConfigManager getInstance() {
        return instance;
    }

    public File getConfigurationDirectory(@Nonnull String workspace) {
        return Paths.get(workspace, AZURE).toFile();
    }

    public File initConfigurationDirectory(@Nonnull String workspace) throws IOException {
        final File result = getConfigurationDirectory(workspace);
        if (!result.exists()) {
            result.mkdirs();
        }
        if (SystemInfo.isWindows) {
            Files.setAttribute(result.toPath(), "dos:hidden", true);
        }
        return result;
    }

    @Nullable
    public CourseConfig getCourseConfigFromWorkspace(@Nonnull Project project) {
        final File directory = getConfigurationDirectory(project.getBasePath());
        final File file = new File(directory, GETTING_START_CONFIGURATION_NAME);
        if (!file.exists()) {
            return null;
        }
        try (final InputStream inputStream = new FileInputStream(file)) {
            return JSON_MAPPER.readValue(inputStream, CourseConfig.class);
        } catch (final IOException e) {
            return null;
        }
    }

    public CourseConfig getCourseByName(@Nonnull final String name) {
        return loadCourses().stream().filter(course -> StringUtils.equalsIgnoreCase(name, course.getName()))
            .findFirst().orElse(null);
    }

    @Cacheable(value = "guidance/courses")
    public List<CourseConfig> loadCourses() {
        return Optional.of(new Reflections("guidance", Scanners.Resources))
            .map(reflections -> {
                try {
                    return reflections.getResources(Pattern.compile(".*\\.json"));
                } catch (final Exception exception) {
                    return Collections.emptySet();
                }
            })
            .orElse(Collections.emptySet())
            .stream().map(uri -> GuidanceConfigManager.loadCourseByUri("/" + uri))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(CourseConfig::getPriority))
            .collect(Collectors.toList());
    }

    @Nullable
    private static CourseConfig loadCourseByUri(String uri) {
        try (final InputStream inputStream = GuidanceConfigManager.class.getResourceAsStream(uri)) {
            final CourseConfig courseConfig = JSON_MAPPER.readValue(inputStream, CourseConfig.class);
            courseConfig.setUri(uri);
            return courseConfig;
        } catch (final IOException e) {
            // swallow exception for failed convertation
            return null;
        }
    }
}
