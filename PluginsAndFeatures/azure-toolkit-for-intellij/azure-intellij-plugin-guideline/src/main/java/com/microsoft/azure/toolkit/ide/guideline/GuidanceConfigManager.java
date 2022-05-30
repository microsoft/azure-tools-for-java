package com.microsoft.azure.toolkit.ide.guideline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.jr.ob.JSON;
import com.microsoft.azure.toolkit.ide.guideline.config.ProcessConfig;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuidanceConfigManager {
    private static final GuidanceConfigManager instance = new GuidanceConfigManager();

    public static GuidanceConfigManager getInstance() {
        return instance;
    }

    @Cacheable(value = "guidance/process")
    public List<ProcessConfig> loadProcessConfig() {
        return Optional.of(new Reflections("guidance", Scanners.Resources))
                .map(reflections -> {
                    try {
                        return reflections.getResources(Pattern.compile(".*\\.yml"));
                    } catch (Exception exception) {
                        return null;
                    }
                })
                .orElse(Collections.emptySet())
                .stream().map(resource -> GuidanceConfigManager.class.getResourceAsStream("/" + resource))
                .filter(Objects::nonNull)
                .map(GuidanceConfigManager::getConfigFromStream).collect(Collectors.toList());
    }

    private static ProcessConfig getConfigFromStream(InputStream inputStream) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
            return mapper.readValue(inputStream, ProcessConfig.class);
        } catch (IOException e) {
            // swallow exception for failed convertation
            return null;
        }
    }
}
