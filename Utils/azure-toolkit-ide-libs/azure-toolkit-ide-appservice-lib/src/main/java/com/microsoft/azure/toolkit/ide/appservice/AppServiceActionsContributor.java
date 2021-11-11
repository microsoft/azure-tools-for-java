/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class AppServiceActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = 1;
    public static final Action.Id<IAppService<?>> OPEN_IN_BROWSER = Action.Id.of("actions.appservice.open_in_browser");
    public static final Action.Id<IAppService<?>> START_STREAM_LOG = Action.Id.of("actions.appservice.stream_log.start");
    public static final Action.Id<IAppService<?>> STOP_STREAM_LOG = Action.Id.of("actions.appservice.stream_log.stop");

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<IAppService<?>> openInBrowser = appService -> am.getAction(ResourceCommonActionsContributor.OPEN_URL)
                .handle("https://" + appService.hostName());
        final ActionView.Builder openInBrowserView = new ActionView.Builder("Open In Browser", "/icons/action/refresh.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("webapp.open_browser")).orElse(null))
                .enabled(s -> s instanceof IAppService);
        am.registerAction(OPEN_IN_BROWSER, new Action<>(openInBrowser, openInBrowserView));

        final ActionView.Builder startStreamLogView = new ActionView.Builder("Start Streaming Logs", "/icons/action/log.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("webapp|app.start_stream_log", ((IAppService) r).name())).orElse(null))
                .enabled(s -> s instanceof IAppService);
        am.registerAction(START_STREAM_LOG, new Action<>(startStreamLogView));

        final ActionView.Builder stopStreamLogView = new ActionView.Builder("Stop Streaming Logs", "/icons/action/log.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("webapp|app.stop_stream_log", ((IAppService) r).name())).orElse(null))
                .enabled(s -> s instanceof IAppService);
        am.registerAction(STOP_STREAM_LOG, new Action<>(stopStreamLogView));
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<IAzureBaseResource<?, ?>, Object> startCondition = (r, e) -> r instanceof IAppService &&
                StringUtils.equals(r.status(), IAzureBaseResource.Status.STOPPED);
        final BiConsumer<IAzureBaseResource<?, ?>, Object> startHandler = (c, e) -> ((IAppService) c).start();
        am.registerHandler(ResourceCommonActionsContributor.START, startCondition, startHandler);

        final BiPredicate<IAzureBaseResource<?, ?>, Object> stopCondition = (r, e) -> r instanceof IAppService &&
                StringUtils.equals(r.status(), IAzureBaseResource.Status.RUNNING);
        final BiConsumer<IAzureBaseResource<?, ?>, Object> stopHandler = (c, e) -> ((IAppService) c).stop();
        am.registerHandler(ResourceCommonActionsContributor.STOP, stopCondition, stopHandler);

        final BiPredicate<IAzureBaseResource<?, ?>, Object> restartCondition = (r, e) -> r instanceof IAppService &&
                StringUtils.equals(r.status(), IAzureBaseResource.Status.RUNNING);
        final BiConsumer<IAzureBaseResource<?, ?>, Object> restartHandler = (c, e) -> ((IAppService) c).restart();
        am.registerHandler(ResourceCommonActionsContributor.RESTART, restartCondition, restartHandler);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
