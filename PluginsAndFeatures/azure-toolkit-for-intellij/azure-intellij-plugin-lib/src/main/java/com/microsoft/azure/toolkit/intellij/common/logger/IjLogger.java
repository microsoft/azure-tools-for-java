package com.microsoft.azure.toolkit.intellij.common.logger;

import com.microsoft.azure.toolkit.lib.common.logger.Logger;
import com.microsoft.azure.toolkit.lib.common.logger.LoggerFactory;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;

@RequiredArgsConstructor
public class IjLogger implements Logger {
    private final com.intellij.openapi.diagnostic.Logger logger;

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }

    @Override
    public void debug(String message, @Nullable Throwable t) {
        this.logger.debug(message, t);
    }

    @Override
    public void info(String message, @Nullable Throwable t) {
        this.logger.info(message, t);
    }

    @Override
    public void warn(String message, @Nullable Throwable t) {
        this.logger.warn(message, t);
    }

    @Override
    public void error(String message, @Nullable Throwable t) {
        this.logger.error(message, t);
    }

    @RequiredArgsConstructor
    public static class Factory extends LoggerFactory {
        @Override
        public synchronized Logger getLogger(Class<?> clazz) {
            return new IjLogger(com.intellij.openapi.diagnostic.Logger.getInstance(clazz));
        }
    }
}