package com.microsoft.azure.toolkit.ide.guideline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guideline.config.ProcessConfig;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuidanceConfigManager {
    public static final String GETTING_START_CONFIGURATION_NAME = "azure-getting-started.yml";

    private static final GuidanceConfigManager instance = new GuidanceConfigManager();
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    public static GuidanceConfigManager getInstance() {
        return instance;
    }

    @Nullable
    public ProcessConfig getProcessConfigFromWorkspace(@Nonnull Project project) {
        final File file = new File(project.getBasePath(), GETTING_START_CONFIGURATION_NAME);
        if (!file.exists()) {
            return null;
        }
        try (final InputStream inputStream = new FileInputStream(file)) {
            return mapper.readValue(inputStream, ProcessConfig.class);
        } catch (final IOException e) {
            return null;
        }
    }

    @Cacheable(value = "guidance/process")
    public List<ProcessConfig> loadProcessConfig() {
        return Optional.of(new Reflections("guidance", Scanners.Resources))
                .map(reflections -> {
                    try {
                        return reflections.getResources(Pattern.compile(".*\\.yml"));
                    } catch (final Exception exception) {
                        return (Set<String>)Collections.EMPTY_SET;
                    }
                })
                .orElse(Collections.emptySet())
                .stream().map(uri -> GuidanceConfigManager.getProcessConfig("/" + uri))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Nullable
    private static ProcessConfig getProcessConfig(String uri) {
        try (final InputStream inputStream = GuidanceConfigManager.class.getResourceAsStream(uri)) {
            final ProcessConfig processConfig = mapper.readValue(inputStream, ProcessConfig.class);
            processConfig.setUri(uri);
            return processConfig;
        } catch (final IOException e) {
            // swallow exception for failed convertation
            return null;
        }
    }
}
