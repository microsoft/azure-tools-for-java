package com.microsoft.azure.toolkit.intellij.cognitiveservices;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessage;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.cognitiveservices.CognitiveServicesActionsContributor.CREATE_DEPLOYMENT;
import static com.microsoft.azure.toolkit.ide.cognitiveservices.CognitiveServicesActionsContributor.OPEN_DEPLOYMENT_IN_PLAYGROUND;
import static com.microsoft.azure.toolkit.intellij.cognitiveservices.IntelliJCognitiveServicesActionsContributor.TRY_OPENAI;
import static com.microsoft.azure.toolkit.intellij.cognitiveservices.IntelliJCognitiveServicesActionsContributor.TRY_PLAYGROUND;

public class CognitiveServicesStartupListener implements ProjectActivity {
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        tryOpenAI(project);
        tryPlayground(project);
        return null;
    }

    private static void tryOpenAI(@Nonnull Project project) {
        if (!IntellijAzureActionManager.isSuppressed(TRY_OPENAI)) {
            final Action<Project> tryOpenAI = new Action<>(TRY_OPENAI)
                .withLabel("Try Azure OpenAI")
                .withHandler((_d, e) -> {
                    GuidanceViewManager.getInstance().openCourseView(project, "hello-openai");
                    IntellijAzureActionManager.suppress(TRY_OPENAI);
                })
                .withAuthRequired(false);
            final AzureString msg = AzureString.format("You can use Azure OpenAI to build your own \"%s\" or other models. " +
                "<a href='https://go.microsoft.com/fwlink/?linkid=2202896&_ijop_=openai.learn_more'>Learn more</a> about Azure OpenAI.", "Copilot");
            final AzureActionManager am = AzureActionManager.getInstance();
            final Action<Action.Id<?>> suppress = Optional.ofNullable(am).map(m -> m.getAction(ResourceCommonActionsContributor.SUPPRESS_ACTION).bind(TRY_OPENAI)).orElse(null);
            final IAzureMessager messager = AzureMessager.getMessager();
            final IntellijAzureMessage message = (IntellijAzureMessage) (Objects.nonNull(suppress) ?
                messager.buildInfoMessage(msg, "Azure OpenAI is supported!", tryOpenAI, suppress) :
                messager.buildInfoMessage(msg, "Azure OpenAI is supported!", tryOpenAI));
            message.setProject(project).show(messager);
        }
    }

    private static void tryPlayground(Project project) {
        AzureEventBus.once("account.subscription_changed.account", (_a, _b) -> {
            final List<CognitiveAccount> accounts = Azure.az(AzureCognitiveServices.class).list().stream()
                .flatMap(m -> m.accounts().list().stream()).toList();
            final Optional<CognitiveDeployment> gptModel = accounts.stream()
                .flatMap(a -> a.deployments().list().stream())
                .filter(d -> d.getModel().isGPTModel())
                .findFirst();
            final IAzureMessager messager = AzureMessager.getMessager();
            gptModel.ifPresentOrElse(d -> {
                if (!IntellijAzureActionManager.isSuppressed(TRY_PLAYGROUND)) {
                    final Action<CognitiveDeployment> tryPlayGround =
                        new Action<>(TRY_PLAYGROUND)
                            .withIdParam(d.getName())
                            .withLabel("Open in AI Playground")
                            .withSource(d)
                            .withHandler((_d, e) -> {
                                AzureActionManager.getInstance().getAction(OPEN_DEPLOYMENT_IN_PLAYGROUND).handle(d, e);
                                IntellijAzureActionManager.suppress(TRY_PLAYGROUND);
                            })
                            .withAuthRequired(true);
                    final Action<Action.Id<?>> suppress = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SUPPRESS_ACTION).bind(TRY_PLAYGROUND);
                    final AzureString msg = AzureString.format("GPT* model based deployment (%s) is detected in your Azure OpenAI service (%s). " +
                        "You can try your own \"%s\" in AI playground.", d.getName(), d.getParent().getName(), "Copilot");
                    final IntellijAzureMessage message = (IntellijAzureMessage) messager.buildInfoMessage(msg, "GPT* model is detected!", tryPlayGround, suppress);
                    message.setProject(project).show(messager);
                }
            }, () -> {
                if (accounts.size() > 0 && !IntellijAzureActionManager.isSuppressed(CREATE_DEPLOYMENT)) {
                    final Action<CognitiveAccount> createDeployment = AzureActionManager.getInstance().getAction(CREATE_DEPLOYMENT).bind(accounts.get(0));
                    final Action<Action.Id<?>> suppress = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SUPPRESS_ACTION).bind(CREATE_DEPLOYMENT);
                    final AzureString msg = AzureString.format("GPT* model based Azure OpenAI deployment can be used to build your own \"%s\". "
                        + "<a href='https://go.microsoft.com/fwlink/?linkid=2202896&_ijop_=openai.learn_more'>Learn more</a> about Azure OpenAI.", "Copilot");
                    final IntellijAzureMessage message = (IntellijAzureMessage) messager.buildInfoMessage(msg, "Build your own \"Copilot\"!", createDeployment, suppress);
                    message.setProject(project).show(messager);
                }
            });
        });
    }
}
