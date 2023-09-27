package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.message.ISenderReceiver;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Optional;

public class SenderReceiverPanel extends JPanel {
    @Getter
    private JPanel contentPanel;
    private AzureActionButton<Void> sendMessageBtn;
    private ExpandableTextField messageInput;
    private JPanel listenPanel;
    private JPanel sendPanel;
    private final ISenderReceiver instance;
    private final ConsoleView consoleView;
    @Nullable
    private RunProcessHandler listenProcessHandler;
    private AzureEventBus.EventListener listener;

    public SenderReceiverPanel(Project project, ISenderReceiver ServiceBusInstance) {
        super();
        this.consoleView = new ConsoleViewImpl(project, true);
        this.instance = ServiceBusInstance;
        $$$setupUI$$$();
        this.init();
    }

    public void startListeningProcess() {
        if (this.instance.isListening()) {
            return;
        }
        this.listenProcessHandler = new RunProcessHandler();
        listenProcessHandler.addDefaultListener();
        listenProcessHandler.startNotify();
        consoleView.attachToProcess(listenProcessHandler);
        final Disposable subscribe = Mono.fromRunnable(this::execute)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        listenProcessHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@Nonnull ProcessEvent event) {
                subscribe.dispose();
            }
        });
    }

    public void stopListeningProcess() {
        this.instance.stopReceivingMessage();
        Optional.ofNullable(this.listenProcessHandler).ifPresent(RunProcessHandler::notifyComplete);
        this.listenProcessHandler = null;
    }

    public void dispose() {
        AzureEventBus.off("resource.status_changed.resource", listener);
    }

    private void init() {
        final GridLayoutManager layout = new GridLayoutManager(1, 1);
        this.setLayout(layout);
        this.add(this.contentPanel, new GridConstraints(0, 0, 1, 1, 0, GridConstraints.ALIGN_FILL, 3, 3, null, null, null, 0));
        this.listenPanel.add(this.consoleView.getComponent(),
                new GridConstraints(0, 0, 1, 1, 0, GridConstraints.ALIGN_FILL,
                        3, 3, null, null, null, 0));
        this.sendMessageBtn.setEnabled(instance.isSendEnabled());
        this.initListeners();
    }

    private void initListeners() {
        this.listener = new AzureEventBus.EventListener((azureEvent) -> {
            final String type = azureEvent.getType();
            final Object source = azureEvent.getSource();
            if (source instanceof ISenderReceiver && ((ISenderReceiver) source).getId().equals(this.instance.getId())) {
                this.sendMessageBtn.setEnabled(instance.isSendEnabled());
            }
        });
        this.sendMessageBtn.addActionListener(e -> sendMessage());
        this.messageInput.addActionListener(e -> sendMessage());
        AzureEventBus.on("resource.status_changed.resource", listener);
    }

    private void sendMessage() {
        final String message = messageInput.getText();
        messageInput.setText(StringUtils.EMPTY);
        AzureTaskManager.getInstance().runInBackground("send message",() -> {
            OperationContext.current().setMessager(new ConsoleMessager(consoleView));
            instance.sendMessage(message);
        });
    }

    private void execute() {
        final ConsoleMessager messager = new ConsoleMessager(consoleView);
        OperationContext.current().setMessager(messager);
        instance.startReceivingMessage();
    }

    private void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        final GridLayoutManager layout = new GridLayoutManager(1, 1);
        this.listenPanel = new JPanel(layout);
    }

    private static class ConsoleMessager extends IntellijAzureMessager {
        private final ConsoleView view;
        public ConsoleMessager(ConsoleView view) {
            super();
            this.view = view;
        }
        @Override
        public boolean show(IAzureMessage msg) {
            final IAzureMessage raw = msg.getRawMessage();
            if (raw.getType() == IAzureMessage.Type.INFO) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.SYSTEM_OUTPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.SUCCESS) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.USER_INPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.DEBUG) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.WARNING) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.LOG_WARNING_OUTPUT);
            } else if (raw.getType() == IAzureMessage.Type.ERROR) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.ERROR_OUTPUT);
            }
            return super.show(raw);
        }

    }
}
