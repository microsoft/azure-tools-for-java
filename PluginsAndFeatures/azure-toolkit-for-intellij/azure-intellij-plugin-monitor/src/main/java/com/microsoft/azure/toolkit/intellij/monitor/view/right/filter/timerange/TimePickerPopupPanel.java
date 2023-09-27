package com.microsoft.azure.toolkit.intellij.monitor.view.right.filter.timerange;

import com.intellij.ui.JBIntSpinner;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.michaelbaranov.microba.calendar.CalendarPane;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;
import java.beans.PropertyVetoException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class TimePickerPopupPanel extends JPanel {
    private JPanel rootPanel;
    private JBIntSpinner hourInput;
    private JBIntSpinner minuteInput;
    private JBIntSpinner secondInput;
    @Getter
    private CalendarPane calendarPane;
    @Getter
    private AzureActionButton<Void> okButton;
    private Date timeDate;

    public TimePickerPopupPanel() {
        super();
        $$$setupUI$$$();
        final GridLayoutManager layout = new GridLayoutManager(1, 1);
        this.setLayout(layout);
        this.add(this.rootPanel, new GridConstraints(0, 0, 1, 1, 0, GridConstraints.ALIGN_FILL, 3, 3, null, null, null, 0));
    }

    public void updateDate(Date date) throws PropertyVetoException {
        this.timeDate = date;
        final Calendar calendarInstance = Calendar.getInstance();
        calendarInstance.setTime(date);
        this.hourInput.setValue(calendarInstance.get(Calendar.HOUR_OF_DAY));
        this.minuteInput.setValue(calendarInstance.get(Calendar.MINUTE));
        this.secondInput.setValue(calendarInstance.get(Calendar.SECOND));
        this.getCalendarPane().setDate(date);
    }

    @Nullable
    public Date getTimeDate() {
        final Calendar calendar = Calendar.getInstance();
        final Date date = calendarPane.getDate();
        if (Objects.isNull(date)) {
            return null;
        }
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hourInput.getNumber());
        calendar.set(Calendar.MINUTE, minuteInput.getNumber());
        calendar.set(Calendar.SECOND, secondInput.getNumber());
        this.timeDate = calendar.getTime();
        return calendar.getTime();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        this.hourInput = new JBIntSpinner(0, 0, 23);
        this.minuteInput = new JBIntSpinner(0, 0, 59);
        this.secondInput = new JBIntSpinner(0, 0, 59);
    }
}
