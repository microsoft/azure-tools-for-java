package com.microsoft.azure.toolkit.intellij.monitor.view.right.filter.timerange;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.util.ui.JBUI;
import com.michaelbaranov.microba.calendar.DatePicker;
import com.michaelbaranov.microba.calendar.resource.Resource;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.swing.*;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.*;

public class TimePicker extends DatePicker {
    private JPopupMenu popup;
    private TimePickerPopupPanel popupPanel;
    private JFormattedTextField field;
    private AzureActionButton<Void> button;
    private ComponentListener componentListener;

    public TimePicker() {
        this(new Date(), 2, Locale.getDefault(), TimeZone.getDefault());
    }

    public TimePicker(Date initialDate, int dateStyle, Locale locale, TimeZone zone) {
        super(initialDate, dateStyle, locale, zone);
        this.initComponents();
    }

    public void showPopup() {
        showPopup(true);
    }

    public void hidePopup() {
        showPopup(false);
    }

    public void updateDate(Date date) throws PropertyVetoException {
        this.field.setValue(date);
    }

    public Date getTimeDate() {
        return Optional.ofNullable(popupPanel).map(TimePickerPopupPanel::getTimeDate).orElse(getDate());
    }

    private void showPopup(boolean visible) {
        if (visible) {
            if (this.isKeepTime()) {
                try {
                    final JFormattedTextField.AbstractFormatter formatter = this.field.getFormatter();
                    final Date value = (Date)formatter.stringToValue(this.field.getText());
                    this.popupPanel.updateDate(value);
                } catch (final Exception ignored) {
                }
            }
            this.popup.show(this, 0, this.getHeight());
            this.popupPanel.requestFocus(false);
        } else {
            this.popup.setVisible(false);
        }
    }

    private void initComponents() {
        this.initPicker();
        this.initPop();
        this.revalidate();
        this.repaint();
        this.initListeners();
        this.peerDateChanged(this.getDate());
    }

    private void initListeners() {
        final Action<Void> pickAction = new Action<Void>(Action.Id.of("user/monitor.pick_time"))
                .withAuthRequired(false)
                .withHandler((Void ignore, AnActionEvent event) -> {
                    final ActionEvent actionEvent = event.getData(AzureActionButton.ACTION_EVENT_KEY);
                    final boolean visible = Objects.nonNull(actionEvent) && actionEvent.getSource() != popupPanel.getCalendarPane();
                    showPopup(visible);
                });
        this.button.setAction(pickAction);
        this.componentListener = new ComponentListener();
        this.field.addPropertyChangeListener(this.componentListener);
        final Action<Void> pickTimeAction = new Action<Void>(Action.Id.of("user/monitor.pick_time"))
                .withAuthRequired(false)
                .withHandler(ignore -> {
                    AzureTaskManager.getInstance().runLater(() -> showPopup(false));
                    final Date timeDate = popupPanel.getTimeDate();
                    Optional.ofNullable(popupPanel.getTimeDate()).ifPresent(t -> this.field.setValue(t));
                });
        this.popupPanel.getOkButton().setAction(pickTimeAction);
        this.addPropertyChangeListener(evt -> {
            if ("dateFormat".equals(evt.getPropertyName())) {
                field.setFormatterFactory(createFormatterFactory());
            }
        });
    }

    private void initPicker() {
        this.field = new JFormattedTextField(this.createFormatterFactory());
        this.field.setValue(this.getDate());
        this.field.setFocusLostBehavior(this.getFocusLostBehavior());
        this.field.setEditable(this.isFieldEditable());
        this.field.setToolTipText(this.getToolTipText());
        this.button = new AzureActionButton<>();
        this.button.setFocusable(false);
        this.button.setMargin(JBUI.emptyInsets());
        this.button.setToolTipText(this.getToolTipText());
        this.setSimpleLook();
        this.setLayout(new BorderLayout());
        this.add(this.field, "Center");
        this.add(this.button, "East");
    }

    private void initPop() {
        this.popupPanel = new TimePickerPopupPanel();
        this.popupPanel.getCalendarPane().setShowTodayButton(false);
        this.popupPanel.getCalendarPane().setShowNoneButton(false);
        this.popupPanel.getCalendarPane().setFocusLostBehavior(2);
        this.popupPanel.setFocusCycleRoot(true);
        this.popupPanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 0, 3));
        this.popupPanel.getCalendarPane().setStripTime(false);
        this.popupPanel.getCalendarPane().setLocale(this.getLocale());
        this.popupPanel.getCalendarPane().setZone(this.getZone());
        this.popupPanel.setFocusable(this.isDropdownFocusable());
        this.popupPanel.getCalendarPane().setColorOverrideMap(this.getColorOverrideMap());
        this.popup = new JPopupMenu();
        this.popup.setLayout(new BorderLayout());
        this.popup.add(this.popupPanel, "Center");
        this.popup.setLightWeightPopupEnabled(true);
    }

    private DefaultFormatterFactory createFormatterFactory() {
        return new DefaultFormatterFactory(new DateFormatter(this.getDateFormat()));
    }

    private void setSimpleLook() {
        this.field.setBorder((new JTextField()).getBorder());
        this.button.setText("");
        this.button.setIcon(new ImageIcon(Resource.class.getResource("picker-16.png")));
    }

    private void peerDateChanged(Date newValue) {
        try {
            this.popupPanel.updateDate(newValue);
        } catch (final Exception ignored) {
        }
        this.field.removePropertyChangeListener(this.componentListener);
        this.field.setValue(newValue);
        this.field.addPropertyChangeListener(this.componentListener);
    }

    private class ComponentListener implements PropertyChangeListener {
        public ComponentListener() {
        }

        public void actionPerformed(ActionEvent e) {
            showPopup(e.getSource() != popupPanel.getCalendarPane());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == field && "value".equals(evt.getPropertyName())) {
                final Date fieldValue = (Date)field.getValue();
                try {
                    popupPanel.updateDate(fieldValue);
                } catch (final PropertyVetoException e) {
                    field.setValue(getDate());
                }
            }
        }
    }

}
