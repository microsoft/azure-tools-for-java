/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.forms.function;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.eventhub.EventHub;
import com.microsoft.azure.management.eventhub.EventHubConsumerGroup;
import com.microsoft.azure.management.eventhub.EventHubNamespace;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.util.AzureFunctionsUtils;
import com.microsoft.intellij.util.ValidationUtils;
import com.microsoft.intellij.wizards.functions.module.FunctionTriggerChooserStep;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CreateFunctionForm extends DialogWrapper implements TelemetryProperties {

    public static final String HTTP_TRIGGER = "HttpTrigger";
    public static final String TIMER_TRIGGER = "TimerTrigger";
    public static final String EVENT_HUB_TRIGGER = "EventHubTrigger";
    public static final String CUSTOMIZED_TIMER_CRON_MESSAGE =
            "Enter a cron expression of the format '{second} {minute} {hour} {day} {month} {day of week}' to specify the schedule";
    public static final String CUSTOMIZED_TIMER_CRON = "Customized Timer Cron";

    private Map<String, JComponent[]> triggerComponents;
    private boolean isSignedIn;
    private JComboBox<String> cbTriggerType;
    private JTextField txtFunctionName;
    private JComboBox<AuthorizationLevel> cbAuthLevel;
    private JTextField txtPackageName;
    private JTextField txtCron;
    private JComboBox cbEventHubNamespace;
    private JTextField txtConnection;
    private JPanel pnlRoot;
    private JLabel lblTriggerType;
    private JLabel lblFunctionName;
    private JLabel lblPackageName;
    private JLabel lblAuthLevel;
    private JLabel lblCron;
    private JLabel lblEventHubNamespace;
    private JLabel lblConnectionName;
    private JComboBox cbFunctionModule;
    private JComboBox cbEventHubName;
    private JComboBox cbConsumerGroup;
    private JLabel lblEventHubName;
    private JLabel lblConsumerGroup;
    private JComboBox cbCron;
    private Project project;

    public CreateFunctionForm(@Nullable Project project, String packageName) {
        super(project, true);
        setModal(true);
        setTitle("Create Function Class");

        this.project = project;
        this.isSignedIn = AuthMethodManager.getInstance().isSignedIn();
        initComponentOfTriggers();

        cbFunctionModule.setRenderer(new SimpleListCellRenderer<Module>() {
            @Override
            public void customize(JList list, Module module, int i, boolean b, boolean b1) {
                if (module != null) {
                    setText(module.getName());
                    setIcon(AllIcons.Nodes.Module);
                }
            }
        });

        cbEventHubNamespace.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof EventHubNamespace) {
                    setText(((EventHubNamespace) object).name());
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        cbEventHubName.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(JList list, Object o, int i, boolean b, boolean b1) {
                if (o instanceof EventHub) {
                    setText(((EventHub) o).name());
                } else if (o instanceof String) {
                    setText(o.toString());
                }
            }
        });

        cbConsumerGroup.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(JList list, Object o, int i, boolean b, boolean b1) {
                if (o instanceof EventHubConsumerGroup) {
                    setText(((EventHubConsumerGroup) o).name());
                } else if (o instanceof String) {
                    setText(o.toString());
                }
            }
        });

        cbCron.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(JList list, Object o, int i, boolean b, boolean b1) {
                if (o instanceof TimerCron) {
                    setText(((TimerCron) o).getDisplay());
                } else if (o instanceof String) {
                    setText(o.toString());
                }
            }
        });


        cbTriggerType.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                onSelectTriggerType();
            }
        });

        cbEventHubNamespace.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                onSelectEventHubNameSpace();
            }
        });

        cbEventHubName.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                onSelectEventHubName();
            }
        });

        cbCron.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                Object selectedCron = cbCron.getSelectedItem();
                if (selectedCron instanceof String && StringUtils.equals((CharSequence) selectedCron, "Customized Schedule")) {
                    AzureTaskManager.getInstance().runLater(() -> addTimer());
                }
            }
        });

        init();
        initTriggers();
        fillModules();
        fillAuthLevel();
        fillTimerSchedule();
        this.txtPackageName.setText(packageName);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public Map<String, String> toProperties() {
        return new HashMap<>();
    }

    public String getTriggerType() {
        return (String) cbTriggerType.getSelectedItem();
    }

    public EventHubNamespace getEventHubNamespace() {
        return (EventHubNamespace) this.cbEventHubNamespace.getSelectedItem();
    }

    public Map<String, String> getTemplateParameters() {
        Map<String, String> result = new HashMap<>();
        result.put("functionName", txtFunctionName.getText());
        result.put("packageName", txtPackageName.getText());
        String className = AzureFunctionsUtils.normalizeClassName(StringUtils.capitalize(txtFunctionName.getText()));
        if (FunctionTriggerChooserStep.SUPPORTED_TRIGGERS.contains(className)) {
            className = className + "1"; // avoid duplicate class with function annotation
        }
        result.put("className", className);
        switch ((String) cbTriggerType.getSelectedItem()) {
            case HTTP_TRIGGER:
                result.put("authLevel", cbAuthLevel.getSelectedItem().toString());
                break;
            case TIMER_TRIGGER:
                if (cbCron.getSelectedItem() instanceof TimerCron) {
                    result.put("schedule", ((TimerCron) cbCron.getSelectedItem()).getValue());
                }
                break;
            case EVENT_HUB_TRIGGER:
                result.put("connection", txtConnection.getText());
                result.put("eventHubName", getSelectedEventHubName());
                result.put("consumerGroup", getConsumerGroupName());
                break;
            default:
                break;
        }
        return result;
    }

    private String getSelectedEventHubName() {
        final EventHub hub = (EventHub) cbEventHubName.getSelectedItem();
        return hub == null ? null : hub.name();
    }

    private String getConsumerGroupName() {
        final EventHubConsumerGroup group = (EventHubConsumerGroup) cbConsumerGroup.getSelectedItem();
        return group == null ? null : group.name();
    }

    private void fillModules() {
        Arrays.stream(ModuleManager.getInstance(project).getModules()).forEach(module -> cbFunctionModule.addItem(module));
    }

    private void initTriggers() {
        triggerComponents.keySet().stream().filter(StringUtils::isNoneBlank).forEach(triggerType -> cbTriggerType.addItem(triggerType));
        hideDynamicComponents();
        onSelectTriggerType();
    }

    private void hideDynamicComponents() {
        triggerComponents.values().forEach(components -> Arrays.stream(components).forEach(jComponent -> jComponent.setVisible(false)));
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> res = new ArrayList<>();
        validateProperties(res, "Package name", txtPackageName, ValidationUtils::isValidJavaPackageName);
        validateProperties(res, "Function name", txtFunctionName, ValidationUtils::isValidAppServiceName);

        final String trigger = (String) cbTriggerType.getSelectedItem();
        if (StringUtils.equals(trigger, EVENT_HUB_TRIGGER)) {
            validateProperties(res, "Connection name", this.txtConnection, StringUtils::isNotBlank);
        }

        return res;
    }

    private static void validateProperties(List<ValidationInfo> res, String propertyName, JTextField textField, Predicate<String> validator) {
        String text = textField.getText();

        if (text.isEmpty()) {
            res.add(new ValidationInfo(propertyName + " is required.", textField));
            return;
        }

        if (!validator.test(text)) {
            res.add(new ValidationInfo(String.format("Invalid %s: %s", propertyName, text), textField));
            return;
        }
    }

    private void initComponentOfTriggers() {
        this.triggerComponents = new HashMap<>();
        triggerComponents.put(HTTP_TRIGGER, new JComponent[]{lblAuthLevel, cbAuthLevel});
        triggerComponents.put(TIMER_TRIGGER, new JComponent[]{lblCron, cbCron});
        if (isSignedIn) {
            triggerComponents.put(EVENT_HUB_TRIGGER, new JComponent[]{lblEventHubNamespace, lblConnectionName, lblEventHubName, lblConsumerGroup,
                cbEventHubNamespace, txtConnection, cbEventHubName, cbConsumerGroup});
        } else {
            triggerComponents.put(EVENT_HUB_TRIGGER, new JComponent[]{lblConnectionName, txtConnection});
            triggerComponents.put("", new JComponent[]{lblEventHubNamespace, lblConnectionName, lblEventHubName, lblConsumerGroup,
                cbEventHubNamespace, txtConnection, cbEventHubName, cbConsumerGroup});
        }
    }

    private void fillAuthLevel() {
        Arrays.stream(AuthorizationLevel.values()).forEach(authLevel -> cbAuthLevel.addItem(authLevel));
    }

    private List<EventHub> getEventHubByNamespaces(EventHubNamespace eventHubNamespace) {
        PagedList<EventHub> result = eventHubNamespace.listEventHubs();
        result.loadAll();
        return result;
    }

    private List<EventHubConsumerGroup> getConsumerGroupByEventHub(EventHub eventHub) {
        PagedList<EventHubConsumerGroup> result = eventHub.listConsumerGroups();
        result.loadAll();
        return result;
    }

    private void fillJComboBox(JComboBox comboBox, Supplier<List<?>> listFunction) {
        fillJComboBox(comboBox, listFunction, null);
    }

    private void fillJComboBox(JComboBox comboBox, Supplier<List<?>> listFunction, Runnable callback) {
        comboBox.removeAllItems();
        comboBox.addItem("Refreshing");
        comboBox.setEnabled(false);

        Observable.fromCallable(() -> listFunction.get()).subscribeOn(Schedulers.newThread())
                .subscribe(functionApps -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    final List list = listFunction.get();
                    comboBox.removeAllItems();
                    comboBox.setEnabled(true);
                    comboBox.setSelectedItem(null);
                    list.forEach(item -> comboBox.addItem(item));
                    if (callback != null) {
                        callback.run();
                    }
                }));
    }

    private List<EventHubNamespace> eventHubNamespaces = null;

    private List<EventHubNamespace> getEventHubNameSpaces() {
        if (eventHubNamespaces == null) {
            eventHubNamespaces = new ArrayList<>();
            final List<Subscription> subs = AzureMvpModel.getInstance().getSelectedSubscriptions();
            for (final Subscription subscriptionId : subs) {
                final Azure azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId.subscriptionId());
                final PagedList<EventHubNamespace> pagedList = azure.eventHubNamespaces().list();
                pagedList.loadAll();
                eventHubNamespaces.addAll(pagedList);
                eventHubNamespaces.sort(Comparator.comparing(HasName::name));
            }
        }
        return eventHubNamespaces;
    }

    private void fillTimerSchedule() {
        Arrays.stream(TimerCron.getDefaultCrons()).forEach(cron -> cbCron.addItem(cron));
        cbCron.addItem("Customized Schedule");
    }

    private void addTimer() {
        String cron = Messages.showInputDialog(project, CUSTOMIZED_TIMER_CRON_MESSAGE, CUSTOMIZED_TIMER_CRON, null);

        if (StringUtils.isBlank(cron)) {
            return;
        }
        TimerCron result = new TimerCron(String.format("Customized: %s", cron), cron);
        cbCron.addItem(result);
        cbCron.setSelectedItem(result);
    }

    private void onSelectTriggerType() {
        final String trigger = (String) cbTriggerType.getSelectedItem();
        hideDynamicComponents();
        if (StringUtils.isNotEmpty(trigger)) {
            Arrays.stream(triggerComponents.get(trigger)).forEach(jComponent -> jComponent.setVisible(true));
        }
        if (trigger.equals(EVENT_HUB_TRIGGER)) {
            fillJComboBox(cbEventHubNamespace, this::getEventHubNameSpaces, this::onSelectEventHubNameSpace);
        }
        CreateFunctionForm.this.pack();
    }

    private void onSelectEventHubNameSpace() {
        Object selectedEventHubNameSpace = cbEventHubNamespace.getSelectedItem();
        if (selectedEventHubNameSpace instanceof EventHubNamespace) {
            fillJComboBox(cbEventHubName, () -> getEventHubByNamespaces((EventHubNamespace) selectedEventHubNameSpace), this::onSelectEventHubName);
            txtConnection.setText(String.format("CONNECTION_%s", ((EventHubNamespace) selectedEventHubNameSpace).name()));
        }
    }

    private void onSelectEventHubName() {
        Object eventHub = cbEventHubName.getSelectedItem();
        if (eventHub instanceof EventHub) {
            fillJComboBox(cbConsumerGroup, () -> getConsumerGroupByEventHub((EventHub) eventHub));
        }
    }

    static class TimerCron {
        // Enter a cron expression of the format '{second} {minute} {hour} {day} {month} {day of week}' to specify the schedule
        public static TimerCron HOURLY = new TimerCron("Hourly", "0 0 * * * *");
        public static TimerCron DAILY = new TimerCron("Daily", "0 0 0 * * *");
        public static TimerCron MONTHLY = new TimerCron("Monthly", "0 0 0 1 * *");

        private String display;
        private String value;

        public TimerCron(String display, String value) {
            this.display = display;
            this.value = value;
        }

        public String getDisplay() {
            return display;
        }

        public String getValue() {
            return value;
        }

        public static TimerCron[] getDefaultCrons() {
            return new TimerCron[]{HOURLY, DAILY, MONTHLY};
        }
    }
}
