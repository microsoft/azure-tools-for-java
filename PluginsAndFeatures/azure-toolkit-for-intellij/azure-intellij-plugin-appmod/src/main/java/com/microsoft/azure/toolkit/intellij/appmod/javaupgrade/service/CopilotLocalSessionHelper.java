/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Creates a fresh LOCAL chat session before sending a prompt via {@code CopilotChatService.query()},
 * ensuring prompts always route to the Local agent regardless of the user's selected provider.
 *
 * <p>On Copilot 1.11+, {@code query(withNewSession())} reuses the home session whose target follows
 * the user's provider selection (BACKGROUND/CLOUD/CLAUDE). If the user has selected a non-Local
 * provider, the prompt lands on the wrong agent. This helper dispatches
 * {@code ChatAction.CreateSession(newId, PANEL, LOCAL, agentMode, null, true)} before the query,
 * creating a race-free LOCAL session. The caller uses {@code withExistingSession(newId)} instead of
 * {@code withNewSession()}.
 *
 * <p>On older Copilot (1.9/1.10) where {@code CreateSession} doesn't exist, this falls back to
 * switching the provider to LOCAL via {@code TargetSelected} or the provider service, and returns
 * null so the caller uses {@code withNewSession()}.
 *
 * <p>All Copilot types are loaded through the Copilot plugin's classloader (since this plugin
 * is not in the same classloader hierarchy) and accessed reflectively so a single binary works
 * across Copilot versions.
 */
public final class CopilotLocalSessionHelper {

    private static final Logger LOG = Logger.getInstance(CopilotLocalSessionHelper.class);

    private static final String COPILOT_PLUGIN_ID = "com.github.copilot";

    private static final String FQ_CHAT_SERVICE = "com.github.copilot.session.ChatService";
    private static final String FQ_CHAT_TYPE = "com.github.copilot.session.Chat$Type";
    private static final String FQ_CHAT_TARGET_TYPE = "com.github.copilot.session.ChatTarget$Type";
    private static final String FQ_CREATE_SESSION = "com.github.copilot.session.ChatAction$CreateSession";
    private static final String FQ_TARGET_SELECTED = "com.github.copilot.session.ChatAction$TargetSelected";
    private static final String FQ_USER_SELECTED_PROVIDER = "com.github.copilot.agent.agentProvider.UserSelectedAgentProviderService";
    private static final String FQ_AGENT_PROVIDER = "com.github.copilot.agent.agentProvider.AgentProvider";
    private static final String FQ_CHATMODE_BUILTINS = "com.github.copilot.agent.chatMode.ChatModeService$BuiltInChatModes";
    private static final String DEFAULT_CTOR_MARKER = "kotlin.jvm.internal.DefaultConstructorMarker";

    private static final String TARGET_LOCAL = "LOCAL";

    private CopilotLocalSessionHelper() {}

    /**
     * Prepares a fresh LOCAL session for the caller's upcoming {@code CopilotChatService.query()}.
     *
     * @param project the current project
     * @return a fresh LOCAL session ID for {@code withExistingSession(id)}, or {@code null} when
     *         the caller should fall back to {@code withNewSession()}.
     */
    @Nullable
    public static String prepareLocalFallbackSession(@Nonnull Project project) {
        ClassLoader cl = getCopilotClassLoader();
        if (cl == null) {
            LOG.debug("prepareLocalFallbackSession: Copilot classloader unavailable.");
            return null;
        }
        try {
            String sessionId = createFreshLocalSessionId(project, cl);
            if (sessionId != null) {
                return sessionId;
            }
        } catch (Throwable t) {
            LOG.debug("Could not create a fresh LOCAL session; falling back to withNewSession.", t);
        }
        // 1.9/1.10 or CreateSession unavailable: try switching provider to LOCAL synchronously.
        try {
            ensureLocalProvider(project, cl);
        } catch (Throwable t) {
            LOG.debug("Failed to switch provider to Local; continuing.", t);
        }
        return null;
    }

    @Nullable
    private static ClassLoader getCopilotClassLoader() {
        IdeaPluginDescriptor copilot = PluginManagerCore.getPlugin(PluginId.getId(COPILOT_PLUGIN_ID));
        if (copilot == null || !copilot.isEnabled()) return null;
        return copilot.getPluginClassLoader();
    }

    /**
     * 1.11+ only: dispatches {@code ChatAction.CreateSession(newId, PANEL, LOCAL, agentMode, null, true)}
     * and returns newId. Returns null when CreateSession is absent (1.9/1.10).
     */
    @Nullable
    private static String createFreshLocalSessionId(@Nonnull Project project, @Nonnull ClassLoader cl) {
        if (classOrNull(cl, FQ_CREATE_SESSION) == null) return null;
        Object chatService = projectService(project, cl, FQ_CHAT_SERVICE);
        if (chatService == null) return null;

        String sessionId = UUID.randomUUID().toString();
        Object panel = enumConst(cl, FQ_CHAT_TYPE, "PANEL");
        Object local = enumConst(cl, FQ_CHAT_TARGET_TYPE, TARGET_LOCAL);
        String agentModeId = builtinAgentModeId(cl);

        Object createAction = constructPadded(cl, FQ_CREATE_SESSION,
                new Object[]{sessionId, panel, local, agentModeId, null, true});
        dispatch(chatService, createAction);
        LOG.info("Created fresh LOCAL session " + sessionId + " for appmod fallback send.");
        return sessionId;
    }

    /**
     * Switches the provider to LOCAL. Tries ChatAction.TargetSelected (1.11+) first, then the
     * UserSelectedAgentProviderService (1.9/1.10).
     */
    private static void ensureLocalProvider(@Nonnull Project project, @Nonnull ClassLoader cl) {
        if (trySwitchViaTargetSelected(project, cl)) {
            LOG.info("Switched provider -> LOCAL via ChatAction.TargetSelected");
            return;
        }
        if (trySwitchViaProviderService(project, cl)) {
            LOG.info("Switched provider -> LOCAL via UserSelectedAgentProviderService");
        }
    }

    private static boolean trySwitchViaTargetSelected(@Nonnull Project project, @Nonnull ClassLoader cl) {
        try {
            Object chatService = projectService(project, cl, FQ_CHAT_SERVICE);
            if (chatService == null) return false;
            Object chatFlow = invoke0OrNull(chatService, "getChat");
            if (chatFlow == null) chatFlow = invoke0OrNull(chatService, "getPanelChat");
            if (chatFlow == null) return false;
            Object chat = invoke0OrNull(chatFlow, "getValue");
            if (chat == null) return false;
            Object activeId = invoke0OrNull(chat, "getActiveSessionId");
            if (!(activeId instanceof String) || ((String) activeId).isEmpty()) return false;
            Object localTarget = enumConst(cl, FQ_CHAT_TARGET_TYPE, TARGET_LOCAL);
            if (localTarget == null) return false;
            Object action = constructByArity(cl, FQ_TARGET_SELECTED, 4,
                    new Object[]{activeId, localTarget, null, null});
            dispatch(chatService, action);
            return true;
        } catch (Throwable t) {
            LOG.debug("Provider switch via ChatAction.TargetSelected failed.", t);
            return false;
        }
    }

    private static boolean trySwitchViaProviderService(@Nonnull Project project, @Nonnull ClassLoader cl) {
        try {
            Object providerService = projectService(project, cl, FQ_USER_SELECTED_PROVIDER);
            if (providerService == null) return false;
            Object local = enumConst(cl, FQ_AGENT_PROVIDER, TARGET_LOCAL);
            if (local == null) return false;
            Method setter = null;
            for (Method m : providerService.getClass().getMethods()) {
                if ("setSelectedAgentProvider".equals(m.getName()) && m.getParameterCount() == 1) {
                    setter = m;
                    break;
                }
            }
            if (setter == null) return false;
            setter.setAccessible(true);
            setter.invoke(providerService, local);
            return true;
        } catch (Throwable t) {
            LOG.debug("Provider switch via UserSelectedAgentProviderService failed.", t);
            return false;
        }
    }

    // ── Reflection utilities (all use Copilot's classloader) ─────────────────

    @Nullable
    private static String builtinAgentModeId(@Nonnull ClassLoader cl) {
        try {
            Object builtins = staticField(cl, FQ_CHATMODE_BUILTINS, "INSTANCE");
            if (builtins == null) return null;
            Object agentMode = invoke0OrNull(builtins, "getAgent");
            if (agentMode == null) return null;
            Object id = invoke0OrNull(agentMode, "getId");
            return id instanceof String ? (String) id : null;
        } catch (Throwable t) {
            LOG.debug("Could not resolve builtin agent mode id.", t);
            return null;
        }
    }

    private static Object constructPadded(@Nonnull ClassLoader cl, String fqcn, Object[] base) {
        try {
            Class<?> clazz = cl.loadClass(fqcn);
            Constructor<?> ctor = canonicalConstructor(clazz);
            if (ctor == null) throw new IllegalStateException("No usable constructor on " + fqcn);
            ctor.setAccessible(true);
            return ctor.newInstance(padArgs(ctor, base));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct " + fqcn, e);
        }
    }

    private static Object constructByArity(@Nonnull ClassLoader cl, String fqcn, int arity, Object[] args) {
        try {
            Class<?> clazz = cl.loadClass(fqcn);
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                if (c.getParameterCount() == arity) {
                    Class<?>[] paramTypes = c.getParameterTypes();
                    if (paramTypes.length > 0 && DEFAULT_CTOR_MARKER.equals(paramTypes[paramTypes.length - 1].getName())) {
                        continue;
                    }
                    c.setAccessible(true);
                    return c.newInstance(args);
                }
            }
            throw new IllegalStateException("No " + arity + "-arg constructor on " + fqcn);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct " + fqcn, e);
        }
    }

    private static void dispatch(Object store, Object action) {
        try {
            Method method = null;
            for (Method m : store.getClass().getMethods()) {
                if ("dispatch".equals(m.getName()) && m.getParameterCount() == 1) {
                    method = m;
                    break;
                }
            }
            if (method == null) throw new IllegalStateException("No dispatch(Action) on " + store.getClass().getName());
            method.setAccessible(true);
            method.invoke(store, action);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("dispatch failed", e);
        }
    }

    @Nullable
    private static Constructor<?> canonicalConstructor(Class<?> clazz) {
        Constructor<?> best = null;
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            Class<?>[] paramTypes = c.getParameterTypes();
            if (paramTypes.length == 0) continue;
            if (DEFAULT_CTOR_MARKER.equals(paramTypes[paramTypes.length - 1].getName())) continue;
            if (best == null || c.getParameterCount() > best.getParameterCount()) {
                best = c;
            }
        }
        return best;
    }

    private static Object[] padArgs(Constructor<?> ctor, Object[] base) {
        Class<?>[] types = ctor.getParameterTypes();
        Object[] out = new Object[types.length];
        System.arraycopy(base, 0, out, 0, base.length);
        for (int i = base.length; i < types.length; i++) {
            out[i] = defaultForType(types[i]);
        }
        return out;
    }

    private static Object defaultForType(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0;
        return 0; // int, byte, short
    }

    @Nullable
    private static Class<?> classOrNull(@Nonnull ClassLoader cl, String fqcn) {
        try {
            return cl.loadClass(fqcn);
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    private static Object projectService(@Nonnull Project project, @Nonnull ClassLoader cl, String fqcn) {
        Class<?> clazz = classOrNull(cl, fqcn);
        if (clazz == null) return null;
        return project.getService(clazz);
    }

    @Nullable
    private static Object enumConst(@Nonnull ClassLoader cl, String fqcn, String name) {
        return staticField(cl, fqcn, name);
    }

    @Nullable
    private static Object staticField(@Nonnull ClassLoader cl, String fqcn, String fieldName) {
        try {
            Class<?> clazz = cl.loadClass(fqcn);
            Field field = clazz.getField(fieldName);
            return field.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    private static Object invoke0OrNull(Object target, String methodName) {
        try {
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    return m.invoke(target);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
