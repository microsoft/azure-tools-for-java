package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guidance.config.InputConfig;
import com.microsoft.azure.toolkit.ide.guidance.config.TaskConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ComponentContext {
    public static final String PARAM_PREFIX = "prefix";
    @Nonnull
    private final Context context;
    @Nonnull
    private final Map<String, String> paramMapping;
    @Nonnull
    private final Map<String, String> resultMapping;

    public ComponentContext(@Nonnull final TaskConfig config, @Nonnull final Context context) {
        this.context = context;
        this.paramMapping = Optional.ofNullable(config.getParamMapping()).orElse(Collections.emptyMap());
        this.resultMapping = Optional.ofNullable(config.getResultMapping()).orElse(Collections.emptyMap());
    }

    public ComponentContext(@Nonnull final InputConfig config, @Nonnull final Context context) {
        this.context = context;
        this.paramMapping = Optional.ofNullable(config.getParamMapping()).orElse(Collections.emptyMap());
        this.resultMapping = Optional.ofNullable(config.getResultMapping()).orElse(Collections.emptyMap());
    }

    @Nonnull
    public Project getProject() {
        return context.getProject();
    }

    public Course getCourse() {
        return context.getCourse();
    }

    @Nullable
    public Object getParameter(@Nonnull final String key) {
        final String mappedKey = paramMapping.getOrDefault(key, key);
        return context.getProperty(mappedKey);
    }

    public void initParameter(@Nonnull final String key, final Object value) {
        final String mappedKey = paramMapping.getOrDefault(key, key);
        context.setProperty(mappedKey, value);
    }

    public void applyResult(@Nonnull final String key, @Nullable final Object value) {
        final String mappedKey = resultMapping.getOrDefault(key, key);
        context.setProperty(mappedKey, value);
    }

    public void addPropertyListener(String key, Consumer<Object> listener) {
        final String mappedKey = paramMapping.getOrDefault(key, key);
        context.addPropertyListener(mappedKey, listener);
    }

    public void removePropertyListener(String key, Consumer<Object> listener) {
        final String mappedKey = paramMapping.getOrDefault(key, key);
        context.removePropertyListener(mappedKey, listener);
    }
}
